package at.fhtw.swen3.paperless.security;

import at.fhtw.swen3.paperless.persistence.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokedTokenCleanupTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void removeExpiredTokens_deletesEntriesThatExpiredBeforeNow() {
        when(revokedTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(2L);
        Instant before = Instant.now();

        new RevokedTokenCleanup(revokedTokenRepository).removeExpiredTokens();

        ArgumentCaptor<Instant> limit = ArgumentCaptor.forClass(Instant.class);
        verify(revokedTokenRepository).deleteByExpiresAtBefore(limit.capture());
        assertThat(limit.getValue()).isBetween(before, Instant.now());
    }
}
