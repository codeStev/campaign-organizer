package com.campaignorganizer.accounts.application.account.port.in;

import java.util.UUID;

public interface ChangeOwnPasswordUseCase {

    void changeOwnPassword(UUID accountId, String currentPassword, String newPassword);
}
