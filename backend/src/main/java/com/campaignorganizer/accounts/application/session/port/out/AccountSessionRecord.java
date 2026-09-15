package com.campaignorganizer.accounts.application.session.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * One row of {@code account_sessions} as stored — {@code id} is the backing token's own
 * {@code jti}. A full application-layer type, not a JPA entity, since it crosses into
 * {@code AccountSessionService} unmodified.
 */
public record AccountSessionRecord(UUID id, UUID accountId, Instant createdAt, String userAgent, String ipAddress,
                                    Instant expiresAt, Instant revokedAt) {
}
