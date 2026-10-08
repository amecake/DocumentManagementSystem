package at.fhtw.swen3.paperless.service.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "username must not be blank") String username,
        @NotBlank(message = "password must not be blank") String password) {
}
