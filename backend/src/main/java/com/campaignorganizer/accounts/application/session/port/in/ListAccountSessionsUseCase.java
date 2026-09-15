package com.campaignorganizer.accounts.application.session.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Self-service listing of the current account's active sessions/devices (ADR-0112). Only
 * currently-active (not revoked, not expired) sessions are returned — a revoked or expired row
 * is already unusable and not useful to show.
 */
public interface ListAccountSessionsUseCase {

    /** {@code currentSessionId} is the caller's own token's session id, used to flag it {@code current}. */
    List<AccountSessionSummary> listSessions(UUID accountId, UUID currentSessionId);
}
