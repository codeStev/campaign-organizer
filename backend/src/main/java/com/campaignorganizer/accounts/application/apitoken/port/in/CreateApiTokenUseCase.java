package com.campaignorganizer.accounts.application.apitoken.port.in;

import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Creates an API token for the calling account; the secret is returned exactly once (ADR-0119). */
public interface CreateApiTokenUseCase {

    CreatedApiToken create(CreateApiTokenCommand command);

    record CreateApiTokenCommand(UUID accountId, String name, Set<ApiTokenScope> scopes, Instant expiresAt) {
    }

    record CreatedApiToken(String secret, ApiTokenView token) {
    }
}
