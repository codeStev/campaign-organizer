package com.campaignorganizer.accounts.application.mfa.port.out;

import java.util.List;
import java.util.UUID;

/**
 * The narrow slice of WebAuthn credential storage {@code MfaService}/{@code
 * WebauthnCredentialService} need — deleting a credential as part of an admin-triggered MFA
 * reset (ADR-0111 follow-up), and self-service listing/removal of an individual credential
 * (ADR-0111 follow-up, self-service credential management). Everything Spring Security's own
 * WebAuthn machinery needs is the separate
 * {@code org.springframework.security.web.webauthn.management.UserCredentialRepository}
 * interface, implemented by the same underlying adapter as this port.
 */
public interface WebAuthnCredentialRepositoryPort {

    boolean existsByAccountId(UUID accountId);

    void deleteByAccountId(UUID accountId);

    /** Ordered by creation date — deliberately excludes the public key/attestation bytes. */
    List<WebAuthnCredentialSummary> findByAccountId(UUID accountId);

    /**
     * Deletes exactly one credential, scoped to the given account — {@code id} is this table's
     * own synthetic row id ({@link WebAuthnCredentialSummary#id()}), never the raw WebAuthn
     * spec credential id (which this port/summary deliberately never exposes). A row id from
     * one account can never delete another account's row, even if guessed.
     *
     * @return true if a matching row was found and deleted, false otherwise (caller decides
     *     whether that's a 404 or a silent no-op)
     */
    boolean deleteByIdForAccount(UUID accountId, UUID id);
}
