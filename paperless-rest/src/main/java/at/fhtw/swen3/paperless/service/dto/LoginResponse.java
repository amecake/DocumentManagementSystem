package at.fhtw.swen3.paperless.service.dto;

/** {@code expiresIn} is in seconds. Send the token as "Authorization: Bearer <accessToken>". */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {
}
