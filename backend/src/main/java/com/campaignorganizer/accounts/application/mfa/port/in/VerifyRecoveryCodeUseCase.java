package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

/**
 * Consumes a recovery code and resets the account's MFA method back to unset (ADR-0111) — see
 * {@code com.campaignorganizer.accounts.application.mfa.service.MfaService}.
 */
public interface VerifyRecoveryCodeUseCase {

    AccountView verifyRecoveryCode(UUID accountId, String recoveryCode);
}
