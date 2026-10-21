package at.fhtw.swen3.paperless.service.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserDto user) {
}
