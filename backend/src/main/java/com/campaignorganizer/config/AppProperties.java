package com.campaignorganizer.config;

import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed access to {@code app.*} configuration.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Media media, Ai ai, Mfa mfa, Webauthn webauthn, Oidc oidc, Foundry foundry) {

    public record Jwt(String secret, long expirationHours) {
    }

    public record Media(String dir) {
    }

    /**
     * Key material for encrypting TOTP secrets at rest (ADR-0111) — never the secrets
     * themselves. {@code encryptionKey} is the password fed into Spring Security Crypto's
     * {@code Encryptors.delux(password, salt)} (AES-256-GCM under a text/Base64 wrapper —
     * {@code Encryptors.stronger(...)} returns the lower-level {@code BytesEncryptor} this
     * wraps, not a {@code TextEncryptor}, so {@code delux} is the one that fits a VARCHAR
     * column directly); {@code encryptionSalt} is a fixed, hex-encoded, non-secret salt for
     * its PBKDF2 key derivation (each encrypted value still gets its own random GCM IV, so
     * one fixed salt for the whole app is the documented, standard usage — not a per-value
     * secret).
     */
    public record Mfa(String encryptionKey, String encryptionSalt) {
    }

    /**
     * WebAuthn relying-party identity (ADR-0111 follow-up). {@code relyingPartyId} must be a
     * registrable domain suffix of whatever origin the browser actually reports (bare hostname,
     * no scheme/port — {@code localhost} in dev); {@code allowedOrigins} must match the
     * browser's origin *exactly* (scheme + host + port). Getting either wrong doesn't error
     * loudly — WebAuthn ceremonies just fail, the single most common integration mistake with
     * this API. Override both per environment; the dev defaults only work for this app's own
     * docker-compose frontend.
     */
    public record Webauthn(String relyingPartyId, String relyingPartyName, Set<String> allowedOrigins) {
    }

    /**
     * AI text-drafting provider secrets (ADR-0064). Either key may be blank — an
     * unconfigured provider is skipped, not attempted and failed. Model choice and
     * provider priority are NOT here — they're user-editable settings (ADR-0065),
     * not deploy-time env config. Base URLs are deploy-time like the keys (they
     * decide where a key is presented — proxy/compatible-endpoint override);
     * blank falls back to each adapter's built-in public endpoint.
     */
    public record Ai(
            String groqApiKey,
            String openRouterApiKey,
            String groqBaseUrl,
            String openRouterBaseUrl) {
    }

    /**
     * Google sign-in (ADR-0113) — {@code SecurityConfig} builds the {@code ClientRegistration}
     * from these by hand rather than via Spring Boot's own {@code
     * spring.security.oauth2.client.registration.*} autoconfiguration, which validates eagerly
     * at bean-creation time and would break the whole app whenever Google isn't configured (see
     * {@code SecurityConfig.clientRegistrationRepository}'s Javadoc). Blank/unset (the default):
     * Google sign-in is entirely absent — no {@code /oauth2/authorization/google}, no unwired
     * filter, no startup dependency on real credentials — same "optional, silently skipped"
     * shape as the AI provider keys.
     */
    public record Oidc(String googleClientId, String googleClientSecret) {

        public boolean googleEnabled() {
            return googleClientId != null && !googleClientId.isBlank();
        }
    }

    /**
     * Key material for encrypting Foundry relay API keys at rest (ADR-0115) — same
     * shape and reasoning as {@link Mfa}'s key/salt, but its own dedicated pair; never
     * reused across secret types. This holds ONLY the encryption key/salt — there is
     * no relay URL, clientId, or API key here or anywhere else at the config/env
     * level. Every world's actual Foundry connection (relay URL, own API key,
     * clientId) is entirely user-supplied and stored per-world; there is no
     * shared/default/bundled relay of any kind.
     */
    public record Foundry(String encryptionKey, String encryptionSalt) {
    }
}
