package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.exception.InvalidFileException;
import at.fhtw.swen3.paperless.exception.UnsupportedFileTypeException;
import at.fhtw.swen3.paperless.service.DocumentService;
import at.fhtw.swen3.paperless.service.dto.DocumentContent;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import at.fhtw.swen3.paperless.service.dto.TagDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private static final String PDF = "application/pdf";
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_LENGTH = 255;

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false)
            @Size(max = MAX_LENGTH, message = "title must be at most 255 characters") String title) throws IOException {
        String filename = validatePdf(file);
        DocumentDto created;
        try (InputStream content = file.getInputStream()) {
            created = documentService.upload(title, filename, file.getSize(), content);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) {
        DocumentContent content = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.filename(), StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(content.stream()));
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAll(@RequestParam(value = "q", required = false) String query) {
        List<DocumentDto> documents = documentService.search(query);
        return documents.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public DocumentDto getById(@PathVariable Long id) {
        return documentService.findById(id);
    }

    @PutMapping("/{id}")
    public DocumentDto update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateDto update) {
        return documentService.update(id, update);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<TagDto> addTag(@PathVariable Long id, @Valid @RequestBody TagDto tag) {
        String name = documentService.addTag(id, tag.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{name}").buildAndExpand(name).toUri();
        return ResponseEntity.created(location).body(new TagDto(name));
    }

    @GetMapping("/{id}/tags")
    public ResponseEntity<List<String>> getTags(@PathVariable Long id) {
        List<String> tags = documentService.getTags(id);
        return tags.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(tags);
    }

    @DeleteMapping("/{id}/tags/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTag(@PathVariable Long id, @PathVariable String name) {
        documentService.removeTag(id, name);
    }

    private static String validatePdf(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidFileException("File must not be empty");
        }
        String filename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!PDF.equals(file.getContentType()) && !filename.toLowerCase().endsWith(".pdf")) {
            throw new UnsupportedFileTypeException("Only PDF documents are supported");
        }
        if (filename.length() > MAX_LENGTH) {
            throw new InvalidFileException("File name must be at most 255 characters");
        }
        if (!hasPdfHeader(file)) {
            throw new UnsupportedFileTypeException("File content is not a valid PDF");
        }
        return filename;
    }

    // content type and file name come from the client, so also check that the file really starts with "%PDF-"
    private static boolean hasPdfHeader(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return Arrays.equals(in.readNBytes(PDF_MAGIC.length), PDF_MAGIC);
        }
    }
}
