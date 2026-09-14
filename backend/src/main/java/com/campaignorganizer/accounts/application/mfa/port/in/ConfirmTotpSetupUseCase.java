package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.MfaEnrollmentOutcome;
import java.util.UUID;

public interface ConfirmTotpSetupUseCase {

    MfaEnrollmentOutcome confirmTotpSetup(UUID accountId, String code);
}
