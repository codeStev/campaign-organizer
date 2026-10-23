package com.campaignorganizer.accounts.application.apitoken.port.in;

import java.util.UUID;

/** Revokes one of the account's own API tokens; another account's id is indistinguishable from unknown (ADR-0119). */
public interface RevokeApiTokenUseCase {

    void revoke(UUID accountId, UUID tokenId);
}
