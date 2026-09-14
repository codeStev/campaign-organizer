package com.campaignorganizer.accounts.adapter.account.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request/response payloads for the MFA setup/verify/recovery endpoints (mirrors docs/api/openapi.yaml). */
public final class MfaWebDtos {

    private MfaWebDtos() {
    }

    public record MfaCodeRequest(@NotBlank String code) {
    }

    public record MfaEnrollmentResult(String token, String tokenType, Instant expiresAt, List<String> recoveryCodes) {
    }

    public record RecoveryCodeRequest(@NotBlank String recoveryCode) {
    }

    public record RecoverPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String recoveryCode,
            @NotBlank @Size(min = 12) String newPassword) {
    }
}
