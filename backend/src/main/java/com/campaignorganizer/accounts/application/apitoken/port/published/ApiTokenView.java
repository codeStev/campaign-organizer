package com.campaignorganizer.accounts.application.apitoken.port.published;

import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Published read model for an API token; never carries the secret or its hash (ADR-0119). */
public record ApiTokenView(
        UUID id,
        UUID accountId,
        int accountTokenVersion,
        String name,
        String prefix,
        Set<ApiTokenScope> scopes,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt) {
}
