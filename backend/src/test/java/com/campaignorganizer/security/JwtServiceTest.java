package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.config.AppProperties;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for token issuing/validation with no Spring context.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    private JwtService jwtServiceWithExpiryHours(long hours) {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt(SECRET, hours),
                new AppProperties.Media("/tmp"),
                new AppProperties.Ai(null, null, null, null), null, null);
        return new JwtService(props);
    }

    @Test
    void issuesFullyAuthenticatedTokenThatParses() {
        JwtService service = jwtServiceWithExpiryHours(1);

        JwtService.IssuedToken issued = service.issue(ACCOUNT_ID, Role.USER, 0);

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(issued.jti()).isNotNull();
        assertThat(service.parse(issued.token())).contains(
                new JwtService.ParsedToken(issued.jti(), ACCOUNT_ID, Role.USER, 0,
                        Set.of(JwtService.PASSWORD_FACTOR, JwtService.MFA_FACTOR)));
    }

    @Test
    void eachIssuedTokenGetsADistinctJti() {
        JwtService service = jwtServiceWithExpiryHours(1);

        JwtService.IssuedToken first = service.issue(ACCOUNT_ID, Role.USER, 0);
        JwtService.IssuedToken second = service.issue(ACCOUNT_ID, Role.USER, 0);

        assertThat(first.jti()).isNotEqualTo(second.jti());
    }

    @Test
    void issuesPasswordOnlyTokenWithOnlyThatFactor() {
        JwtService service = jwtServiceWithExpiryHours(1);

        JwtService.IssuedToken issued = service.issue(ACCOUNT_ID, Role.USER, 0, Set.of(JwtService.PASSWORD_FACTOR));

        assertThat(service.parse(issued.token())).contains(
                new JwtService.ParsedToken(issued.jti(), ACCOUNT_ID, Role.USER, 0, Set.of(JwtService.PASSWORD_FACTOR)));
    }

    @Test
    void passwordOnlyTokenExpiresSoonerThanFullyAuthenticatedToken() {
        JwtService service = jwtServiceWithExpiryHours(1);

        JwtService.IssuedToken full = service.issue(ACCOUNT_ID, Role.USER, 0);
        JwtService.IssuedToken pending = service.issue(ACCOUNT_ID, Role.USER, 0, Set.of(JwtService.PASSWORD_FACTOR));

        assertThat(pending.expiresAt()).isBefore(full.expiresAt());
    }

    @Test
    void rejectsGarbageToken() {
        JwtService service = jwtServiceWithExpiryHours(1);

        assertThat(service.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService issuer = jwtServiceWithExpiryHours(1);
        AppProperties otherProps = new AppProperties(
                new AppProperties.Jwt("a-completely-different-secret-32-bytes-xx", 1),
                new AppProperties.Media("/tmp"),
                new AppProperties.Ai(null, null, null, null), null, null);
        JwtService verifier = new JwtService(otherProps);

        String token = issuer.issue(ACCOUNT_ID, Role.USER, 0).token();

        assertThat(verifier.parse(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = jwtServiceWithExpiryHours(-1); // already expired

        String token = service.issue(ACCOUNT_ID, Role.USER, 0).token();

        assertThat(service.parse(token)).isEmpty();
    }
}
