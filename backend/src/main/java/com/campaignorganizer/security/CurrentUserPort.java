package com.campaignorganizer.security;

import com.campaignorganizer.accounts.domain.account.Role;
import java.util.UUID;

/**
 * Who is making this request, per the already-authenticated security
 * context — not an authorization decision itself (see
 * {@link WorldPermissionEvaluator} for that), just a "who am I" reader used
 * by services that scope their own list/create operations to the current
 * account (e.g. {@code WorldService.list/create}).
 */
public interface CurrentUserPort {

    UUID currentAccountId();

    Role currentRole();
}
