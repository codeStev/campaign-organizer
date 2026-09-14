package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

public interface VerifyTotpChallengeUseCase {

    AccountView verifyTotpChallenge(UUID accountId, String code);
}
