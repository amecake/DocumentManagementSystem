package at.fhtw.swen3.paperless.service.dto;

import java.time.Instant;
import java.util.List;

public record DocumentDto(
        Long id,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant uploadedAt,
        Instant updatedAt,
        List<String> tags,
        String ocrStatus) {
}
