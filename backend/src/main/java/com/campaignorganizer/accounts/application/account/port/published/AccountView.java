package com.campaignorganizer.accounts.application.account.port.published;

import com.campaignorganizer.accounts.domain.account.Role;
import java.time.Instant;
import java.util.UUID;

/** Published read model for an account. Never carries the password hash. */
public record AccountView(
        UUID id,
        String email,
        Role role,
        boolean enabled,
        int tokenVersion,
        Instant createdAt) {
}
