package com.campaignorganizer.auth;

import java.time.Instant;

/** Response of {@code POST /api/auth/login}. */
public record TokenResponse(String token, String tokenType, Instant expiresAt) {

    public static TokenResponse bearer(String token, Instant expiresAt) {
        return new TokenResponse(token, "Bearer", expiresAt);
    }
}
