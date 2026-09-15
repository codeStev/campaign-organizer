package com.campaignorganizer.auth;

/** Response of {@code GET /api/auth/oidc/status} — lets the frontend hide the Google button entirely when unconfigured. */
public record OidcStatusResponse(boolean googleEnabled) {
}
