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

import java.util.Optional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // hash that is checked when the user does not exist, so that unknown users take as long as known ones
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-protection");
    }

    @Override
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

    @Override
    public LoginResponse login(LoginRequest request) {
        Optional<UserEntity> found = userRepository.findByUsernameIgnoreCase(request.username().trim());
        // always run one BCrypt check, otherwise the response time shows whether the user exists
        String hash = found.map(UserEntity::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        UserEntity user = found.filter(u -> passwordMatches)
                .orElseThrow(() -> {
                    log.warn("Failed login for username '{}'", request.username());
                    return new InvalidCredentialsException();
                });
        log.info("User {} logged in", user.getId());
        return new LoginResponse(jwtService.issue(user), "Bearer", jwtService.getValidity().toSeconds(), toDto(user));
    }

    @Override
    public void logout(String token) {
        jwtService.revoke(token);
        log.info("Token revoked (logout)");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return userRepository.findById(id)
                .map(AuthServiceImpl::toDto)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    private static UserDto toDto(UserEntity user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
