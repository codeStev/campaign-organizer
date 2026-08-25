package com.campaignorganizer.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.config.AppProperties;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for token issuing/validation with no Spring context.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-bytes-long";

    private JwtService jwtServiceWithExpiryHours(long hours) {
        AppProperties props = new AppProperties(
                "pw",
                new AppProperties.Jwt(SECRET, hours),
                new AppProperties.Media("/tmp"),
                new AppProperties.Ai(null, null, null, null));
        return new JwtService(props);
    }

    @Test
    void issuesTokenThatValidates() {
        JwtService service = jwtServiceWithExpiryHours(1);

        JwtService.IssuedToken issued = service.issue();

        assertThat(issued.token()).isNotBlank();
        assertThat(issued.expiresAt()).isAfter(Instant.now());
        assertThat(service.isValid(issued.token())).isTrue();
    }

    @Test
    void rejectsGarbageToken() {
        JwtService service = jwtServiceWithExpiryHours(1);

        assertThat(service.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService issuer = jwtServiceWithExpiryHours(1);
        AppProperties otherProps = new AppProperties(
                "pw",
                new AppProperties.Jwt("a-completely-different-secret-32-bytes-xx", 1),
                new AppProperties.Media("/tmp"),
                new AppProperties.Ai(null, null, null, null));
        JwtService verifier = new JwtService(otherProps);

        String token = issuer.issue().token();

        assertThat(verifier.isValid(token)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = jwtServiceWithExpiryHours(-1); // already expired

        String token = service.issue().token();

        assertThat(service.isValid(token)).isFalse();
    }
}
