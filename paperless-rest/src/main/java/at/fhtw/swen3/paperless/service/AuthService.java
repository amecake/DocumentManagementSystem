package at.fhtw.swen3.paperless.service;

import at.fhtw.swen3.paperless.exception.ConflictException;
import at.fhtw.swen3.paperless.exception.InvalidCredentialsException;
import at.fhtw.swen3.paperless.exception.NotFoundException;
import at.fhtw.swen3.paperless.persistence.entity.UserEntity;
import at.fhtw.swen3.paperless.persistence.repository.UserRepository;
import at.fhtw.swen3.paperless.security.JwtService;
import at.fhtw.swen3.paperless.service.dto.LoginRequest;
import at.fhtw.swen3.paperless.service.dto.LoginResponse;
import at.fhtw.swen3.paperless.service.dto.RegisterRequest;
import at.fhtw.swen3.paperless.service.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserDto register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(request.email().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        UserEntity saved = userRepository.save(user);
        log.info("User registered: id={}, username={}", saved.getId(), saved.getUsername());
        return toDto(saved);
    }

    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> {
                    // same message for unknown user and wrong password, so usernames can't be guessed
                    log.warn("Failed login for username '{}'", request.username());
                    return new InvalidCredentialsException();
                });
        log.info("User {} logged in", user.getId());
        return new LoginResponse(jwtService.issue(user), "Bearer", jwtService.getValidity().toSeconds(), toDto(user));
    }

    public void logout(String token) {
        jwtService.revoke(token);
        log.info("Token revoked (logout)");
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return userRepository.findById(id)
                .map(AuthService::toDto)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    private static UserDto toDto(UserEntity user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
