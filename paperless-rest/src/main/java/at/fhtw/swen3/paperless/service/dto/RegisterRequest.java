package at.fhtw.swen3.paperless.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username must not be blank")
        @Size(min = 3, max = 50, message = "username must be 3 to 50 characters")
        String username,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid e-mail address")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        // BCrypt only uses the first 72 bytes of a password
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
        String password) {
}
