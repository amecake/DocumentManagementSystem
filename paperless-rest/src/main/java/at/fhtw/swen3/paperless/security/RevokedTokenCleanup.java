package at.fhtw.swen3.paperless.security;

import at.fhtw.swen3.paperless.persistence.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * A revoked token is only interesting until it expires (an expired token is rejected anyway),
 * so older rows are removed regularly. The interval can be changed with security.jwt.cleanup-interval.
 */
@Component
public class RevokedTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RevokedTokenCleanup.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedTokenCleanup(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Scheduled(fixedDelayString = "${security.jwt.cleanup-interval:PT1H}")
    @Transactional
    public void removeExpiredTokens() {
        long removed = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Removed {} expired entries from revoked_tokens", removed);
        }
    }
}
