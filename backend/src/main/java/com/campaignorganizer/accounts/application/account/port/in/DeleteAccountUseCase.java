package com.campaignorganizer.accounts.application.account.port.in;

import java.util.UUID;

public interface DeleteAccountUseCase {

    void delete(UUID accountId);
}
