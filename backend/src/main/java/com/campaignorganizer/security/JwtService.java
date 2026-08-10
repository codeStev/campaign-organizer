package com.campaignorganizer.security;

import com.campaignorganizer.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the single-owner bearer token.
 * See docs/adr/0006-single-password-auth.md.
 */
@Service
public class JwtService {

    private static final String SUBJECT = "owner";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(properties.jwt().expirationHours());
    }

    public IssuedToken issue() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(SUBJECT)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * @return true when the token is a valid, unexpired owner token.
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
