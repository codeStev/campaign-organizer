package com.campaignorganizer.accounts.application.account.port.published;

import java.util.Optional;
import java.util.UUID;

/** Per-request lookup used by {@code JwtAuthFilter} for token-version/enabled checks — not a login. */
public interface AccountQueryPort {

    Optional<AccountView> findById(UUID accountId);
}
