package com.campaignorganizer.accounts.application.oidc.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived, single-use handoff for the OIDC login redirect (ADR-0113) — the success handler
 * can't write the resulting {@code LoginResponse} directly onto a full-page browser redirect
 * response, and putting a bearer token in the redirect URL itself would land it in browser
 * history and, for a query param, server access logs and the Referer header. Staging it behind
 * an opaque code the frontend immediately exchanges avoids that entirely.
 */
public interface OidcLoginExchangePort {

    /** @return a fresh, unguessable code the caller redirects the browser with */
    UUID stage(String loginResponseJson, Instant expiresAt);

    /** Atomically finds and deletes — a code can be redeemed at most once. Empty if missing/expired/already used. */
    Optional<String> consume(UUID code);
}
