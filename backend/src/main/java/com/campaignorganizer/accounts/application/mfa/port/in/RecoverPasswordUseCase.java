package com.campaignorganizer.accounts.application.mfa.port.in;

/**
 * Resets a forgotten password using a recovery code instead of the current password — no
 * email infrastructure involved (ADR-0111). Always takes comparable time whether or not the
 * email exists, mirroring {@code AccountService.authenticate}'s anti-enumeration handling
 * (ADR-0110).
 */
public interface RecoverPasswordUseCase {

    void recoverPassword(String email, String recoveryCode, String newPassword);
}
