package at.fhtw.swen3.paperless.controller;

import at.fhtw.swen3.paperless.service.AuthService;
import at.fhtw.swen3.paperless.service.dto.LoginRequest;
import at.fhtw.swen3.paperless.service.dto.LoginResponse;
import at.fhtw.swen3.paperless.service.dto.RegisterRequest;
import at.fhtw.swen3.paperless.service.dto.UserDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Resources: a user (POST /users creates one) and a session (POST /sessions logs in,
 * DELETE /sessions/current logs out).
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/sessions")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @DeleteMapping("/sessions/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.substring("Bearer ".length()));
    }

    @GetMapping("/users/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserDto me(Authentication authentication) {
        return authService.getUser(Long.valueOf(authentication.getName()));
    }
}
