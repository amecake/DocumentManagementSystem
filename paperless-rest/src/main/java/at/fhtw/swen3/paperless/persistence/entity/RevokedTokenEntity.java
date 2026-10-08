package at.fhtw.swen3.paperless.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

/** A JWT that was logged out before it expired. */
@Entity
@Table(name = "revoked_tokens")
public class RevokedTokenEntity {

    @Id
    @Column(name = "token_id", length = 36)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedTokenEntity() {}

    public RevokedTokenEntity(String tokenId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public Instant getExpiresAt() { return expiresAt; }
}
