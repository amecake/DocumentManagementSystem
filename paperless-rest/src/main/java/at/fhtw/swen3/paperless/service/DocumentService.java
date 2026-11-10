package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.service.dto.DocumentDto;
import at.fhtw.swen3.paperless.service.dto.DocumentUpdateDto;

import java.util.List;

public interface DocumentService {

    DocumentDto upload(String title, String filename, long fileSize);

    List<DocumentDto> findAll();

    DocumentDto findById(Long id);

    List<DocumentDto> search(String query);

    DocumentDto update(Long id, DocumentUpdateDto update);

    void delete(Long id);

    String addTag(Long documentId, String tagName);

    void removeTag(Long documentId, String tagName);

    List<String> getTags(Long documentId);
}
