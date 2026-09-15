package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

/**
 * Confirms a TOTP secret replacement started via {@link StartTotpReEnrollmentUseCase}
 * (ADR-0111 follow-up). No recovery-code reissuance — which specific secret backs the "MFA"
 * factor is independent of the account's standing recovery-code batch. The caller's own token
 * *is* reissued (the returned {@link AccountView} carries the post-bump token version): the
 * bumped version invalidates every other outstanding token for this account (e.g. a lost or
 * stolen device's still-live session — the whole point of this flow), so the caller needs a
 * fresh one for their own request to keep working past this call.
 */
public interface ConfirmTotpReEnrollmentUseCase {

    AccountView confirmTotpReEnrollment(UUID accountId, String code);
}
