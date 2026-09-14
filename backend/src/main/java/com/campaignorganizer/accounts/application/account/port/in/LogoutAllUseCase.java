package com.campaignorganizer.accounts.application.account.port.in;

import java.util.UUID;

public interface LogoutAllUseCase {

    void logoutAll(UUID accountId);
}
