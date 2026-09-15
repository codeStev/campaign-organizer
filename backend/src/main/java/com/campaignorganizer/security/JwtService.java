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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Issues and validates per-account bearer tokens. A token's {@code factors} claim names which
 * authentication factors it carries — {@link #PASSWORD_FACTOR} alone, or also
 * {@link #MFA_FACTOR} once a second factor has been completed. A correct password never grants
 * full access on its own (ADR-0111, mandatory MFA): {@code com.campaignorganizer.config.SecurityConfig}
 * requires both factors, as {@link FactorGrantedAuthority} instances, for real API access — see
 * {@link JwtAuthFilter} for where a token's {@code factors} claim becomes those authorities.
 * See also docs/adr/0110-self-registration-and-role-based-jwt.md and
 * docs/adr/0111-mandatory-mfa.md.
 */
@Service
public class JwtService {

    /** {@link FactorGrantedAuthority#PASSWORD_AUTHORITY}'s bare claim value (not the "FACTOR_" authority string). */
    public static final String PASSWORD_FACTOR = "PASSWORD";
    /** Granted regardless of which second factor (TOTP or WebAuthn) actually proved it. */
    public static final String MFA_FACTOR = "MFA";
    /** {@code FactorGrantedAuthority.withFactor(MFA_FACTOR).build().getAuthority()}, precomputed for SecurityConfig. */
    public static final String MFA_AUTHORITY = "FACTOR_" + MFA_FACTOR;

    private static final String ROLE_CLAIM = "role";
    private static final String VERSION_CLAIM = "ver";
    private static final String FACTORS_CLAIM = "factors";

    /** A token missing the MFA factor is only ever used to finish logging in — keep its blast radius small. */
    private static final Duration PENDING_MFA_EXPIRATION = Duration.ofMinutes(10);

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofHours(properties.jwt().expirationHours());
    }

    /** Issues a fully-authenticated token: both the PASSWORD and MFA factors. */
    public IssuedToken issue(UUID accountId, Role role, int tokenVersion) {
        return issue(accountId, role, tokenVersion, Set.of(PASSWORD_FACTOR, MFA_FACTOR));
    }

    /**
     * Issues a token carrying exactly the given factors. Used directly whenever the caller
     * hasn't completed MFA yet (a bare {@code Set.of(PASSWORD_FACTOR)} token, short-lived and
     * only usable against {@code /auth/mfa/**} per the {@code multiFactor()} authorization rule
     * in {@link SecurityConfig}).
     */
    public IssuedToken issue(UUID accountId, Role role, int tokenVersion, Set<String> factors) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(factors.contains(MFA_FACTOR) ? expiration : PENDING_MFA_EXPIRATION);
        UUID jti = UUID.randomUUID();
        String token = Jwts.builder()
                .id(jti.toString())
                .subject(accountId.toString())
                .claim(ROLE_CLAIM, role.name())
                .claim(VERSION_CLAIM, tokenVersion)
                .claim(FACTORS_CLAIM, List.copyOf(factors))
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt, jti);
    }

    /** Parses and verifies the token's signature/expiry; empty if invalid, expired, or malformed. */
    @SuppressWarnings("unchecked")
    public Optional<ParsedToken> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID jti = UUID.fromString(claims.getId());
            UUID accountId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get(ROLE_CLAIM, String.class));
            int tokenVersion = claims.get(VERSION_CLAIM, Integer.class);
            Set<String> factors = Set.copyOf(claims.get(FACTORS_CLAIM, List.class));
            return Optional.of(new ParsedToken(jti, accountId, role, tokenVersion, factors));
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return Optional.empty();
        }
    }

    /** {@code jti} is this token's own id, used as the primary key of a full token's {@code account_sessions} row. */
    public record IssuedToken(String token, Instant expiresAt, UUID jti) {
    }

    public record ParsedToken(UUID jti, UUID accountId, Role role, int tokenVersion, Set<String> factors) {
    }
}
