package com.campaignorganizer.accounts.application.account.port.in;

import java.util.UUID;

/** Admin sets a new password for another account directly (no email infra exists to do a reset-link flow). */
public interface ResetPasswordUseCase {

    void resetPassword(UUID accountId, String newPassword);
}
