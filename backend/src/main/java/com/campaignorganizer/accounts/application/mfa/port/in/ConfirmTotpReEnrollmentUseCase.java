package com.campaignorganizer.accounts.application.mfa.port.in;

import java.util.UUID;

/**
 * Confirms a TOTP secret replacement started via {@link StartTotpReEnrollmentUseCase}
 * (ADR-0111 follow-up). No token or recovery-code reissuance — the caller is already fully
 * authenticated, and which specific secret backs the "MFA" factor is independent of the
 * account's standing recovery-code batch.
 */
public interface ConfirmTotpReEnrollmentUseCase {

    void confirmTotpReEnrollment(UUID accountId, String code);
}
