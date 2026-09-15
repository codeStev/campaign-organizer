package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.RecoveryCodeStatus;
import java.util.UUID;

/** Self-service visibility into how many recovery codes are left (ADR-0111 follow-up). */
public interface GetRecoveryCodeStatusUseCase {

    RecoveryCodeStatus getRecoveryCodeStatus(UUID accountId);
}
