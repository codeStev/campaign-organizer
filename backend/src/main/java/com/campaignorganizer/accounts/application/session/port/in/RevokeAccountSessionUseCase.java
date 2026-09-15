package com.campaignorganizer.accounts.application.session.port.in;

import java.util.UUID;

/**
 * Self-service revocation of one of the current account's own sessions/devices (ADR-0112) —
 * including the caller's own current session, a legitimate "log out this device" that simply
 * means the very next request with that token is rejected. No lockout guard is needed here
 * (unlike WebAuthn credential removal): revoking every session doesn't threaten permanent
 * lockout, since password login can always mint a fresh one.
 */
public interface RevokeAccountSessionUseCase {

    void revokeSession(UUID accountId, UUID sessionId);
}
