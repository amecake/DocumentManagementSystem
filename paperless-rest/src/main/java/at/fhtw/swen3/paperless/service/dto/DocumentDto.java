package at.fhtw.swen3.paperless.service.dto;

import java.time.Instant;

public record DocumentDto(
        Long id,
        String title,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant uploadedAt,
        Instant updatedAt) {
}
