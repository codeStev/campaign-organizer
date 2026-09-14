package com.campaignorganizer.security;

import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates per-account bearer tokens. See
 * docs/adr/0110-self-registration-and-role-based-jwt.md.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String VERSION_CLAIM = "ver";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(properties.jwt().expirationHours());
    }

    public IssuedToken issue(UUID accountId, Role role, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(accountId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(VERSION_CLAIM, tokenVersion)
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /** Parses and verifies the token's signature/expiry; empty if invalid, expired, or malformed. */
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID accountId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
            int tokenVersion = claims.get(VERSION_CLAIM, Integer.class);
            return Optional.of(new ParsedToken(accountId, role, tokenVersion));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public record ParsedToken(UUID accountId, Role role, int tokenVersion) {
    }
}
