package at.fhtw.swen3.paperless.security;

import at.fhtw.swen3.paperless.persistence.entity.RevokedTokenEntity;
import at.fhtw.swen3.paperless.persistence.entity.UserEntity;
import at.fhtw.swen3.paperless.persistence.repository.RevokedTokenRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies JWTs. Logging out stores the token id, so a revoked token is rejected until it expires. */
@Service
public class JwtService {

    private final Algorithm algorithm;
    private final Duration validity;
    private final RevokedTokenRepository revokedTokenRepository;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.validity}") Duration validity,
                      RevokedTokenRepository revokedTokenRepository) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.validity = validity;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public String issue(UserEntity user) {
        Instant now = Instant.now();
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(String.valueOf(user.getId()))
                .withClaim("username", user.getUsername())
                .withIssuedAt(now)
                .withExpiresAt(now.plus(validity))
                .sign(algorithm);
    }

    /** Returns the decoded token if the signature is valid, it is not expired and it was not logged out. */
    public Optional<DecodedJWT> verify(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            if (revokedTokenRepository.existsById(jwt.getId())) {
                return Optional.empty();
            }
            return Optional.of(jwt);
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        verify(token).ifPresent(jwt ->
                revokedTokenRepository.save(new RevokedTokenEntity(jwt.getId(), jwt.getExpiresAtAsInstant())));
    }

    public Duration getValidity() {
        return validity;
    }
}
