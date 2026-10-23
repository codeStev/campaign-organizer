package com.campaignorganizer.accounts.adapter.account.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Request/response payloads for API tokens (mirrors docs/api/openapi.yaml); scopes use their wire values. */
public final class ApiTokenWebDtos {

    private ApiTokenWebDtos() {
    }

    public record CreateApiTokenRequest(
            @NotBlank @Size(max = 100) String name,
            @NotEmpty Set<String> scopes,
            Instant expiresAt) {
    }

    public record ApiTokenSummary(
            UUID id,
            String name,
            String prefix,
            List<String> scopes,
            Instant createdAt,
            Instant expiresAt,
            Instant lastUsedAt) {
    }

    public record CreatedApiToken(String token, ApiTokenSummary summary) {
    }
}
