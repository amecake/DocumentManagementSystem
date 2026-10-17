package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.messaging.DocumentUploadedEvent;
import at.fhtw.swen3.paperless.messaging.OcrJobMessage;
import at.fhtw.swen3.paperless.messaging.OcrResultMessage;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.ProcessingStatus;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.persistence.repository.TagRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final String PDF = "application/pdf";

    private final DocumentRepository documentRepository;
    private final DocumentMapper mapper;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher events;

    public DocumentService(DocumentRepository documentRepository, DocumentMapper mapper, TagRepository tagRepository,
                           ApplicationEventPublisher events) {
        this.documentRepository = documentRepository;
        this.mapper = mapper;
        this.tagRepository = tagRepository;
        this.events = events;
    }

    public DocumentDto upload(String title, String filename, long fileSize) {
        DocumentEntity entity = new DocumentEntity();
        entity.setTitle(StringUtils.hasText(title) ? title.trim() : filename);
        entity.setOriginalFilename(filename);
        entity.setContentType(PDF);
        entity.setFileSize(fileSize);

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Document uploaded: id={}, file={}, size={} bytes", saved.getId(), filename, fileSize);
        events.publishEvent(new DocumentUploadedEvent(saved.getId(), filename));
        return mapper.toDto(saved);
    }

    /** Result from the worker. The document may have been deleted in the meantime. */
    public void applyOcrResult(OcrResultMessage result) {
        Optional<DocumentEntity> found = documentRepository.findById(result.documentId());
        if (found.isEmpty()) {
            log.warn("OCR result for unknown document {} ignored", result.documentId());
            return;
        }
        DocumentEntity entity = found.get();
        entity.setProcessedAt(Instant.now());
        if (result.success()) {
            entity.setStatus(ProcessingStatus.COMPLETED);
            entity.setOcrText(result.text());
            log.info("OCR completed for document {}", entity.getId());
        } else {
            entity.setStatus(ProcessingStatus.FAILED);
            log.warn("OCR failed for document {}: {}", entity.getId(), result.error());
        }
        documentRepository.save(entity);
    }

    /** Documents still QUEUED after {@code olderThan}; queuedAt is reset so they aren't resent every run. */
    public List<OcrJobMessage> markStuckForRetry(Duration olderThan) {
        Instant now = Instant.now();
        List<DocumentEntity> stuck = documentRepository
                .findByStatusAndQueuedAtBefore(ProcessingStatus.QUEUED, now.minus(olderThan));
        stuck.forEach(doc -> doc.setQueuedAt(now));
        documentRepository.saveAll(stuck);
        return stuck.stream()
                .map(doc -> new OcrJobMessage(doc.getId(), doc.getOriginalFilename()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> findAll() {
        return mapper.toDtos(documentRepository.findAll());
    }

    @Transactional(readOnly = true)
    public DocumentDto findById(Long id) {
        return mapper.toDto(getDocument(id));
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> search(String query) {
        if (!StringUtils.hasText(query)) {
            return findAll();
        }
        return mapper.toDtos(documentRepository.findByTitleContainingIgnoreCase(query.trim()));
    }

    public DocumentDto update(Long id, DocumentUpdateDto update) {
        DocumentEntity entity = getDocument(id);
        entity.setTitle(update.title().trim());
        log.info("Document {} metadata updated", id);
        return mapper.toDto(documentRepository.save(entity));
    }

    public void delete(Long id) {
        DocumentEntity entity = getDocument(id);
        documentRepository.delete(entity);
        log.info("Document {} deleted", id);
    }

    /** Adds a tag; the name is trimmed and lower-cased so "Finance" and " finance " are the same tag. */
    public String addTag(Long documentId, String tagName) {
        DocumentEntity document = getDocument(documentId);
        String name = normalize(tagName);
        TagEntity tag = tagRepository.findByName(name)
                .orElseGet(() -> tagRepository.save(new TagEntity(name)));
        if (!document.getTags().add(tag)) {
            throw new ConflictException("Document " + documentId + " already has tag '" + name + "'");
        }
        log.info("Tag '{}' added to document {}", name, documentId);
        return name;
    }

    /** Removing a tag the document does not have is not an error (DELETE is idempotent). */
    public void removeTag(Long documentId, String tagName) {
        DocumentEntity document = getDocument(documentId);
        String name = normalize(tagName);
        if (document.getTags().removeIf(tag -> tag.getName().equals(name))) {
            log.info("Tag '{}' removed from document {}", name, documentId);
        }
    }

    @Transactional(readOnly = true)
    public List<String> getTags(Long documentId) {
        return getDocument(documentId).getTags().stream()
                .map(TagEntity::getName)
                .sorted()
                .toList();
    }

    private static String normalize(String tagName) {
        return tagName.trim().toLowerCase(Locale.ROOT);
    }

    private DocumentEntity getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }
}
