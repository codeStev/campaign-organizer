package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.TotpSetupStart;
import java.util.UUID;

public interface StartTotpSetupUseCase {

    TotpSetupStart startTotpSetup(UUID accountId);
}
