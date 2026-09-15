package com.campaignorganizer.accounts.application.session.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a new session row at the moment a full (PASSWORD+MFA) token is issued (ADR-0112) —
 * called from every call site that mints one: {@code MfaController}'s TOTP/WebAuthn
 * enrollment-confirm and challenge-verify handlers, {@code AccountAdminController}'s TOTP
 * re-enrollment confirm, and {@code WebAuthnAuthenticationSuccessHandler}. Deliberately NOT
 * called for a PASSWORD-only pending-MFA token (login, recovery-code verify) — that token isn't
 * a "device logged in" yet, and {@code JwtAuthFilter} never checks session-activity for it.
 */
public interface RecordAccountSessionUseCase {

    void recordSession(UUID accountId, UUID sessionId, Instant expiresAt, String userAgent, String ipAddress);
}
