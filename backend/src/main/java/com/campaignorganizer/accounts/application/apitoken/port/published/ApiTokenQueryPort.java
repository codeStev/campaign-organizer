package com.campaignorganizer.accounts.application.apitoken.port.published;

import java.util.Optional;

/**
 * Used by {@code ApiTokenAuthFilter} to resolve a presented secret to a usable token (ADR-0119).
 * Empty for an unknown, revoked or expired secret; the caller still has to check the account.
 */
public interface ApiTokenQueryPort {

    Optional<ApiTokenView> authenticate(String secret);
}
