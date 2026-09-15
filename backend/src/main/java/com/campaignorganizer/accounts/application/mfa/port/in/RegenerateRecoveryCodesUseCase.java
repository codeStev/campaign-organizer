package com.campaignorganizer.accounts.application.mfa.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Self-service recovery-code regeneration (ADR-0111 follow-up) — invalidates every
 * outstanding code and issues a fresh batch, shown once. Doesn't touch {@code mfaMethod} or
 * issue a new token: the caller already holds a full PASSWORD+MFA session to reach this.
 */
public interface RegenerateRecoveryCodesUseCase {

    List<String> regenerateRecoveryCodes(UUID accountId);
}
