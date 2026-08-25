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
     * AI text-drafting provider config (ADR-0064). Either key may be blank —
     * an unconfigured provider is skipped, not attempted and failed.
     */
    public record Ai(String groqApiKey, String groqModel, String openRouterApiKey, String openRouterModel) {
    }
}
