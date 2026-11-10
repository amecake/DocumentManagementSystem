package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.messaging.DocumentEventPublisher;
import at.fhtw.swen3.paperless.messaging.DocumentUploadedEvent;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.entity.OcrStatus;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.persistence.repository.TagRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);
    private static final String PDF = "application/pdf";

    private final DocumentRepository documentRepository;
    private final DocumentMapper mapper;
    private final TagRepository tagRepository;
    private final DocumentEventPublisher eventPublisher;

    public DocumentServiceImpl(DocumentRepository documentRepository, DocumentMapper mapper,
                               TagRepository tagRepository, DocumentEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.mapper = mapper;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public DocumentDto upload(String title, String filename, long fileSize) {
        DocumentEntity entity = new DocumentEntity();
        entity.setTitle(StringUtils.hasText(title) ? title.trim() : filename);
        entity.setOriginalFilename(filename);
        entity.setContentType(PDF);
        entity.setFileSize(fileSize);
        entity.setOcrStatus(OcrStatus.PENDING);

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Document uploaded: id={}, file={}, size={} bytes", saved.getId(), filename, fileSize);

        boolean queued = eventPublisher.publish(new DocumentUploadedEvent(saved.getId(), saved.getTitle(), filename));
        if (!queued) {
            // the upload itself worked, but nobody will process the document: make that visible
            saved.setOcrStatus(OcrStatus.FAILED);
            log.warn("Document {} saved, but the OCR message could not be sent (ocrStatus=FAILED)", saved.getId());
        }
        return mapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> findAll() {
        return mapper.toDtos(documentRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto findById(Long id) {
        return mapper.toDto(getDocument(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentDto> search(String query) {
        if (!StringUtils.hasText(query)) {
            return findAll();
        }
        return mapper.toDtos(documentRepository.findByTitleContainingIgnoreCase(query.trim()));
    }

    @Override
    public DocumentDto update(Long id, DocumentUpdateDto update) {
        DocumentEntity entity = getDocument(id);
        entity.setTitle(update.title().trim());
        log.info("Document {} metadata updated", id);
        // flush so that @PreUpdate sets updatedAt before the entity is mapped to the DTO
        return mapper.toDto(documentRepository.saveAndFlush(entity));
    }

    @Override
    public void delete(Long id) {
        DocumentEntity entity = getDocument(id);
        documentRepository.delete(entity);
        log.info("Document {} deleted", id);
    }

    @Override
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

    @Override
    public void removeTag(Long documentId, String tagName) {
        DocumentEntity document = getDocument(documentId);
        String name = normalize(tagName);
        if (document.getTags().removeIf(tag -> tag.getName().equals(name))) {
            log.info("Tag '{}' removed from document {}", name, documentId);
        }
    }

    @Override
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
