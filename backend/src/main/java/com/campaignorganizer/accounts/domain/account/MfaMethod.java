package com.campaignorganizer.accounts.domain.account;

/**
 * An account's active second factor. NONE means the account has not yet
 * completed enrollment — its tokens carry only the PASSWORD authentication
 * factor, which is not enough for real API access (ADR-0111, mandatory
 * MFA, no opt-out).
 */
public enum MfaMethod {
    NONE,
    TOTP,
    WEBAUTHN
}
