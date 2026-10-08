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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private UserEntity user(String password) {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setUsername("jarod");
        u.setEmail("jarod@example.com");
        u.setPasswordHash(passwordEncoder.encode(password));
        return u;
    }

    @Test
    void register_storesHashedPassword() {
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserDto result = service.register(new RegisterRequest(" jarod ", "jarod@example.com", "secret123"));

        assertThat(result.username()).isEqualTo("jarod");
        verify(userRepository).save(argThat(u ->
                !u.getPasswordHash().equals("secret123") && passwordEncoder.matches("secret123", u.getPasswordHash())));
    }

    @Test
    void register_takenUsername_throwsConflict() {
        when(userRepository.existsByUsernameIgnoreCase("jarod")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("jarod", "a@b.at", "secret123")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_correctPassword_returnsToken() {
        when(userRepository.findByUsernameIgnoreCase("jarod")).thenReturn(Optional.of(user("secret123")));
        when(jwtService.issue(any())).thenReturn("token");
        when(jwtService.getValidity()).thenReturn(Duration.ofHours(1));

        LoginResponse response = service.login(new LoginRequest("jarod", "secret123"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    void login_wrongPassword_throws() {
        when(userRepository.findByUsernameIgnoreCase("jarod")).thenReturn(Optional.of(user("secret123")));

        assertThatThrownBy(() -> service.login(new LoginRequest("jarod", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    void login_unknownUser_throws() {
        when(userRepository.findByUsernameIgnoreCase("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void logout_revokesToken() {
        service.logout("token");

        verify(jwtService).revoke("token");
    }

    @Test
    void getUser_missing_throwsNotFound() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(5L)).isInstanceOf(NotFoundException.class);
    }
}
