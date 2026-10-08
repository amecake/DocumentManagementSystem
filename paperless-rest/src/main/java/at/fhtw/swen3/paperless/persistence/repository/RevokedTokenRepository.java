package at.fhtw.swen3.paperless.persistence.repository;

import at.fhtw.swen3.paperless.persistence.entity.RevokedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {
}
