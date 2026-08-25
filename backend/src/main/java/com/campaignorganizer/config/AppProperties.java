package com.campaignorganizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed access to {@code app.*} configuration.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String password, Jwt jwt, Media media, Ai ai) {

    public record Jwt(String secret, long expirationHours) {
    }

    public record Media(String dir) {
    }

    /**
     * AI text-drafting provider secrets (ADR-0064). Either key may be blank — an
     * unconfigured provider is skipped, not attempted and failed. Model choice and
     * provider priority are NOT here — they're user-editable settings (ADR-0065),
     * not deploy-time env config.
     */
    public record Ai(String groqApiKey, String openRouterApiKey) {
    }
}
