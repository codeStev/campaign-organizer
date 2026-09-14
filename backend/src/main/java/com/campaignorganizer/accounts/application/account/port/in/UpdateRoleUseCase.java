package com.campaignorganizer.accounts.application.account.port.in;

import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

public interface UpdateRoleUseCase {

    AccountView updateRole(UUID accountId, Role role);
}
