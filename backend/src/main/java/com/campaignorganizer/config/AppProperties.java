package com.campaignorganizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed access to {@code app.*} configuration.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String password, Jwt jwt, Media media) {

    public record Jwt(String secret, long expirationHours) {
    }

    public record Media(String dir) {
    }
}
