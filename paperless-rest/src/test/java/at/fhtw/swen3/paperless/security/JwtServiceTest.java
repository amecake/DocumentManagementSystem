package at.fhtw.swen3.paperless.security;

import at.fhtw.swen3.paperless.persistence.entity.RevokedTokenEntity;
import at.fhtw.swen3.paperless.persistence.entity.UserEntity;
import at.fhtw.swen3.paperless.persistence.repository.RevokedTokenRepository;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret", Duration.ofHours(1), revokedTokenRepository);
    }

    private static UserEntity user() {
        UserEntity u = new UserEntity();
        u.setId(42L);
        u.setUsername("jarod");
        return u;
    }

    @Test
    void issuedToken_canBeVerified() {
        String token = jwtService.issue(user());

        DecodedJWT jwt = jwtService.verify(token).orElseThrow();

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaim("username").asString()).isEqualTo("jarod");
    }

    @Test
    void tokenSignedWithOtherSecret_isRejected() {
        String token = new JwtService("other-secret", Duration.ofHours(1), revokedTokenRepository).issue(user());

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    void expiredToken_isRejected() {
        String token = new JwtService("test-secret", Duration.ofSeconds(-10), revokedTokenRepository).issue(user());

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    void garbage_isRejected() {
        assertThat(jwtService.verify("not-a-token")).isEmpty();
    }

    @Test
    void revoke_storesTokenId_andTokenIsRejectedAfterwards() {
        String token = jwtService.issue(user());
        String tokenId = jwtService.verify(token).orElseThrow().getId();

        jwtService.revoke(token);

        ArgumentCaptor<RevokedTokenEntity> saved = ArgumentCaptor.forClass(RevokedTokenEntity.class);
        verify(revokedTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenId()).isEqualTo(tokenId);

        when(revokedTokenRepository.existsById(tokenId)).thenReturn(true);
        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    void revoke_invalidToken_storesNothing() {
        jwtService.revoke("not-a-token");

        verify(revokedTokenRepository, never()).save(any());
    }
}
