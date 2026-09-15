package com.campaignorganizer.accounts.application.mfa.port.in;

import java.util.UUID;

/**
 * Self-service removal of one of the current account's registered WebAuthn credentials
 * (ADR-0111 follow-up) — refuses to remove the account's last remaining credential while
 * WebAuthn is still its active MFA method, since the domain has no representation for
 * "WEBAUTHN with zero credentials" and that state would effectively lock the account out.
 */
public interface RemoveWebauthnCredentialUseCase {

    void removeWebauthnCredential(UUID accountId, UUID credentialId);
}
