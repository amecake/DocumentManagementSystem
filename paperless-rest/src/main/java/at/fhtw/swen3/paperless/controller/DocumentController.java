package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.service.DocumentService;
import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.Set;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "title", required = false) String title) {
        return documentService.upload(title, file);
    }

    @GetMapping
    public List<DocumentDto> getAll() {
        return documentService.findAll();
    }

    @GetMapping("/search")
    public List<DocumentDto> search(@RequestParam("q") String query) {
        return documentService.search(query);
    }

    @GetMapping("/{id}")
    public DocumentDto getById(@PathVariable Long id) {
        return documentService.findById(id);
    }

    @PutMapping("/{id}")
    public DocumentDto update(@PathVariable Long id, @Valid @RequestBody DocumentUpdateDto update) {
        return documentService.update(id, update);
    }

    @PostMapping("/{id}/tags")
    public Set<String> addTag(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return documentService.addTag(id, body.get("name"));
    }

    @GetMapping("/{id}/tags")
    public Set<String> getTags(@PathVariable Long id) {
        return documentService.getTags(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }
}
