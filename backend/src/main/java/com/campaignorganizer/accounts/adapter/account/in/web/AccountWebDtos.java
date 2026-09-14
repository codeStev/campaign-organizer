package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Request/response payloads for the accounts resource (mirrors docs/api/openapi.yaml). */
public final class AccountWebDtos {

    private AccountWebDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 12) String password) {
    }

    public record RegistrationAccepted(String message) {
    }

    public record AccountResponse(
            UUID id,
            String email,
            Role role,
            boolean enabled,
            MfaMethod mfaMethod,
            Instant createdAt) {
    }

    public record UpdateRoleRequest(@NotNull Role role) {
    }

    public record ResetPasswordRequest(@NotBlank @Size(min = 12) String newPassword) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12) String newPassword) {
    }
}
