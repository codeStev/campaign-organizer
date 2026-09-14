package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.List;

public final class MfaResults {

    private MfaResults() {
    }

    public record TotpSetupStart(String secret, String provisioningUri, String qrCodeDataUri) {
    }

    /**
     * The account after a completed enrollment (both PASSWORD and MFA factors now provable),
     * plus its recovery codes in plaintext — the only time they're ever available; the caller
     * must show them now.
     */
    public record MfaEnrollmentOutcome(AccountView account, List<String> recoveryCodes) {
    }
}
