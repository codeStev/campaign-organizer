package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.MfaEnrollmentOutcome;
import java.util.UUID;

/**
 * Completes WebAuthn enrollment after Spring Security's own {@code POST /webauthn/register}
 * has already verified and stored the credential (ADR-0111 follow-up) — no code parameter is
 * needed here, unlike TOTP's confirm step, since the cryptographic proof already happened.
 */
public interface ConfirmWebauthnSetupUseCase {

    MfaEnrollmentOutcome confirmWebauthnSetup(UUID accountId);
}
