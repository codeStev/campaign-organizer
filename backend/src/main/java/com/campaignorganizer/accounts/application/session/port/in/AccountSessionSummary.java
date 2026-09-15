package com.campaignorganizer.accounts.application.session.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * A self-service-visible session, safe to expose over HTTP (ADR-0112) — {@code current} is
 * request-specific (whether {@code id} matches the caller's own token's session), computed by
 * {@link ListAccountSessionsUseCase} rather than being a property of the stored row itself.
 */
public record AccountSessionSummary(UUID id, Instant createdAt, String userAgent, String ipAddress,
                                     Instant expiresAt, boolean current) {
}
