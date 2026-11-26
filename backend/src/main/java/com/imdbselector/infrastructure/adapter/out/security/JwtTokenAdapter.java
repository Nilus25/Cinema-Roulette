package com.imdbselector.infrastructure.adapter.out.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.imdbselector.application.port.out.TokenPort;
import com.imdbselector.domain.valueobject.UserId;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class JwtTokenAdapter implements TokenPort {
    private static final String ISSUER = "cinema-roulette";
    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final Clock clock;
    private final Duration ttl;

    public JwtTokenAdapter(String secret, Clock clock, Duration ttl) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("JWT secret must be at least 16 characters");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.clock = Objects.requireNonNull(clock);
        this.ttl = Objects.requireNonNull(ttl);
    }

    @Override
    public String issueToken(UserId userId, String username) {
        Instant now = clock.instant();
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.value().toString())
                .withClaim("username", username)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(ttl))
                .sign(algorithm);
    }

    @Override
    public Optional<UserId> verifyToken(String token) {
        try {
            var decoded = verifier.verify(token);
            return Optional.of(UserId.fromString(decoded.getSubject()));
        } catch (JWTVerificationException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
