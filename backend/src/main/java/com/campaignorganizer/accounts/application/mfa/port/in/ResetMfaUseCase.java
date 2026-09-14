package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import java.util.UUID;

/**
 * Admin-assisted recovery for an account whose MFA is unusable — lost device with no
 * recovery codes left, or an enrollment the admin believes wasn't done by the legitimate
 * owner (ADR-0111: a leaked password alone is enough to complete TOTP enrollment, since this
 * app has no out-of-band channel to verify identity beyond the password itself; this is the
 * recovery path once an admin notices). Clears the account's MFA method and secrets and
 * invalidates every outstanding recovery code — a recovery code the wrong person holds must
 * stop working immediately, not just the method flag — and bumps the token version, forcing
 * re-enrollment on next login.
 */
public interface ResetMfaUseCase {

    AccountView resetMfa(UUID accountId);
}
