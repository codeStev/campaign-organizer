package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.in.MfaResults.TotpSetupStart;
import java.util.UUID;

/**
 * Self-service TOTP replacement for an account whose active method is already TOTP (ADR-0111
 * follow-up) — e.g. a new phone. Deliberately a separate use case from {@link
 * StartTotpSetupUseCase} rather than a loosened version of it: this one is reachable only via
 * a route requiring the MFA factor already proven, so the two can never be confused into
 * accepting each other's starting account state.
 */
public interface StartTotpReEnrollmentUseCase {

    TotpSetupStart startTotpReEnrollment(UUID accountId);
}
