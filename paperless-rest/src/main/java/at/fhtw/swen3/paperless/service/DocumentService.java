package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.InvalidFileException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.persistence.entity.DocumentEntity;
import at.fhtw.swen3.paperless.persistence.repository.DocumentRepository;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.mapper.DocumentMapper;
import at.fhtw.swen3.paperless.persistence.entity.TagEntity;
import at.fhtw.swen3.paperless.persistence.repository.TagRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final String PDF = "application/pdf";

    private final DocumentRepository documentRepository;
    private final DocumentMapper mapper;
    private final TagRepository tagRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentMapper mapper, TagRepository tagRepository) {
        this.documentRepository = documentRepository;
        this.mapper = mapper;
        this.tagRepository = tagRepository;
    }

    public DocumentDto upload(String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File must not be empty");
        }
        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!PDF.equals(file.getContentType()) && !filename.toLowerCase().endsWith(".pdf")) {
            throw new InvalidFileException("Only PDF documents are supported");
        }

        DocumentEntity entity = new DocumentEntity();
        entity.setTitle(StringUtils.hasText(title) ? title.trim() : filename);
        entity.setOriginalFilename(filename);
        entity.setContentType(PDF);
        entity.setFileSize(file.getSize());

        DocumentEntity saved = documentRepository.save(entity);
        log.info("Document uploaded: id={}, file={}, size={} bytes", saved.getId(), filename, file.getSize());
        return mapper.toDto(saved);
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

    public Set<String> addTag(Long documentId, String tagName) {
        DocumentEntity document = getDocument(documentId);
        TagEntity tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(new TagEntity(tagName)));
        document.getTags().add(tag);
        documentRepository.save(document);
        log.info("Tag '{}' added to document {}", tagName, documentId);
        return document.getTags().stream().map(TagEntity::getName).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<String> getTags(Long documentId) {
        DocumentEntity document = getDocument(documentId);
        return document.getTags().stream().map(TagEntity::getName).collect(Collectors.toSet());
    }

    private DocumentEntity getDocument(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Document " + id + " not found"));
    }
}
