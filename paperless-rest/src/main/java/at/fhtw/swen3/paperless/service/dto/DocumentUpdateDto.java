package at.fhtw.swen3.paperless.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Metadata the user is allowed to change. */
public record DocumentUpdateDto(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title) {
}
