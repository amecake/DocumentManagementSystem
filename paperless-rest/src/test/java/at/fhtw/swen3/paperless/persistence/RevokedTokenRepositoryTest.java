package at.fhtw.swen3.paperless.persistence;

import at.fhtw.swen3.paperless.persistence.entity.RevokedTokenEntity;
import at.fhtw.swen3.paperless.persistence.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RevokedTokenRepositoryTest {

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void deleteByExpiresAtBefore_removesOnlyExpiredTokens() {
        Instant now = Instant.now();
        revokedTokenRepository.save(new RevokedTokenEntity("expired", now.minus(Duration.ofHours(1))));
        revokedTokenRepository.save(new RevokedTokenEntity("still-valid", now.plus(Duration.ofHours(1))));

        long removed = revokedTokenRepository.deleteByExpiresAtBefore(now);

        assertThat(removed).isEqualTo(1);
        assertThat(revokedTokenRepository.existsById("expired")).isFalse();
        assertThat(revokedTokenRepository.existsById("still-valid")).isTrue();
    }
}
