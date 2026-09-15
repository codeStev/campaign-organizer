package com.campaignorganizer.accounts.application.mfa.port.out;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * A registered WebAuthn credential, safe to expose over HTTP — deliberately excludes the
 * public key, attestation object/client-data, and the raw WebAuthn spec credential id (the
 * WebAuthn spec itself calls out that credential ids can leak identifying information if
 * exposed carelessly; {@code id} here is this table's own synthetic row id instead).
 */
public record WebAuthnCredentialSummary(UUID id, String label, Instant createdAt, Instant lastUsedAt,
                                         Set<String> transports) {
}
