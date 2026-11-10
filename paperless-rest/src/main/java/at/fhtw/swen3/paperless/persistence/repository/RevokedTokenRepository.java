package at.fhtw.swen3.paperless.persistence.repository;

import at.fhtw.swen3.paperless.persistence.entity.RevokedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {

    /** Deletes all entries whose token has expired anyway and returns how many were removed. */
    long deleteByExpiresAtBefore(Instant time);
}
