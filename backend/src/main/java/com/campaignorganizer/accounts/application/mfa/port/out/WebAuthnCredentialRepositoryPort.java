package com.campaignorganizer.accounts.application.mfa.port.out;

import java.util.UUID;

/**
 * The narrow slice of WebAuthn credential storage {@code MfaService} itself needs — deleting a
 * credential as part of an admin-triggered MFA reset (ADR-0111 follow-up). Everything Spring
 * Security's own WebAuthn machinery needs is the separate
 * {@code org.springframework.security.web.webauthn.management.UserCredentialRepository}
 * interface, implemented by the same underlying adapter as this port.
 */
public interface WebAuthnCredentialRepositoryPort {

    boolean existsByAccountId(UUID accountId);

    void deleteByAccountId(UUID accountId);
}
