package com.campaignorganizer.accounts.application.session.port.published;

import java.util.UUID;

/**
 * Per-request lookup used by {@code JwtAuthFilter} to confirm a full-factor token's session
 * hasn't been revoked or expired (ADR-0112) — the finer-grained sibling of {@code
 * AccountQueryPort}'s {@code tokenVersion} check. Never consulted for a PASSWORD-only token,
 * which carries no session row at all.
 */
public interface AccountSessionQueryPort {

    boolean isActive(UUID sessionId);
}
