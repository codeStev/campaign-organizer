package com.campaignorganizer.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The caller's own address, for display purposes (e.g. a session's recorded IP) — not used for
 * any security decision, so no need for {@code RateLimitFilter}'s stricter trust requirements.
 */
public final class ClientAddress {

    private ClientAddress() {
    }

    /** {@code X-Forwarded-For} first, since a reverse proxy sits in front of this app in every real deployment. */
    public static String of(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
