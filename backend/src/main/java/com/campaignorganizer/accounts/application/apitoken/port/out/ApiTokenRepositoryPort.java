package com.campaignorganizer.accounts.application.apitoken.port.out;

import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiTokenRepositoryPort {

    ApiToken save(ApiToken token);

    Optional<ApiToken> findByTokenHash(String tokenHash);

    Optional<ApiToken> findByIdAndAccountId(UUID tokenId, UUID accountId);

    /** Non-revoked tokens only, oldest first. */
    List<ApiToken> findActiveByAccountId(UUID accountId);
}
