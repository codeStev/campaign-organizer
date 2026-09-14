package com.campaignorganizer.auth;

import com.campaignorganizer.accounts.domain.account.MfaMethod;
import java.time.Instant;

/**
 * Response of {@code POST /api/auth/login}. The token always carries only the PASSWORD
 * authentication factor (ADR-0111) — it's usable against {@code /auth/mfa/**} per
 * {@code status}, not general API access, until a second factor is also completed.
 */
public record LoginResponse(LoginStatus status, String token, String tokenType, Instant expiresAt,
                            MfaMethod method) {

    public static LoginResponse setupRequired(String token, Instant expiresAt) {
        return new LoginResponse(LoginStatus.MFA_SETUP_REQUIRED, token, "Bearer", expiresAt, null);
    }

    public static LoginResponse challengeRequired(String token, Instant expiresAt, MfaMethod method) {
        return new LoginResponse(LoginStatus.MFA_CHALLENGE_REQUIRED, token, "Bearer", expiresAt, method);
    }
}
