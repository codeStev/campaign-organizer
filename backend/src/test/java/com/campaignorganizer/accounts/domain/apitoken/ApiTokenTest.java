package com.campaignorganizer.accounts.domain.apitoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit coverage for the API token aggregate (ADR-0119). */
class ApiTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    private static ApiToken token(Instant expiresAt) {
        return ApiToken.create(UUID.randomUUID(), UUID.randomUUID(), 0, "recorder", "hash", "cot_abcd",
                Set.of(ApiTokenScope.GLOSSARY_READ), expiresAt, NOW);
    }

    @Test
    void aNewTokenWithoutExpiryIsUsableAndAllowsOnlyItsScopes() {
        ApiToken token = token(null);

        assertThat(token.isUsable(NOW.plusSeconds(86_400L * 365))).isTrue();
        assertThat(token.allows(ApiTokenScope.GLOSSARY_READ)).isTrue();
        assertThat(token.allows(ApiTokenScope.SESSIONS_WRITE)).isFalse();
    }

    @Test
    void aTokenStopsBeingUsableAtItsExpiry() {
        ApiToken token = token(NOW.plusSeconds(60));

        assertThat(token.isUsable(NOW.plusSeconds(59))).isTrue();
        assertThat(token.isUsable(NOW.plusSeconds(60))).isFalse();
    }

    @Test
    void aRevokedTokenIsNotUsableAndRevokingTwiceKeepsTheFirstTime() {
        ApiToken token = token(null);

        token.revoke(NOW.plusSeconds(10));
        token.revoke(NOW.plusSeconds(20));

        assertThat(token.isUsable(NOW.plusSeconds(30))).isFalse();
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void usageIsRecordedAtMostOncePerResolutionWindow() {
        ApiToken token = token(null);

        assertThat(token.recordUse(NOW)).isTrue();
        assertThat(token.recordUse(NOW.plusSeconds(60))).isFalse();
        assertThat(token.recordUse(NOW.plus(ApiToken.LAST_USED_RESOLUTION))).isTrue();
        assertThat(token.getLastUsedAt()).isEqualTo(NOW.plus(ApiToken.LAST_USED_RESOLUTION));
    }

    @Test
    void rejectsBlankOrOverlongNames() {
        Set<ApiTokenScope> scopes = Set.of(ApiTokenScope.GLOSSARY_READ);
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> ApiToken.create(id, id, 0, " ", "hash", "cot_abcd", scopes, null, NOW))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> ApiToken.create(id, id, 0, "x".repeat(101), "hash", "cot_abcd", scopes, null, NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEmptyScopesAndPastExpiry() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> ApiToken.create(id, id, 0, "n", "hash", "cot_abcd", Set.of(), null, NOW))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> token(NOW)).isInstanceOf(ValidationException.class);
    }

    @Test
    void scopesRoundTripThroughTheirWireValue() {
        for (ApiTokenScope scope : ApiTokenScope.values()) {
            assertThat(ApiTokenScope.fromWireValue(scope.wireValue())).isEqualTo(scope);
        }
        assertThatThrownBy(() -> ApiTokenScope.fromWireValue("accounts:admin"))
                .isInstanceOf(ValidationException.class);
    }
}
