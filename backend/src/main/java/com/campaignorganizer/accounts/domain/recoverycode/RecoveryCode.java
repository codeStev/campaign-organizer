package com.campaignorganizer.accounts.domain.recoverycode;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A single-use MFA recovery code (ADR-0111), hashed the same way as an account password —
 * never stored or compared in plaintext. Serves two purposes: regaining access after a lost
 * second factor, and resetting a forgotten password without email infrastructure.
 */
public final class RecoveryCode {

    private final UUID id;
    private final UUID accountId;
    private final String codeHash;
    private Instant usedAt;
    private final Instant createdAt;

    private RecoveryCode(UUID id, UUID accountId, String codeHash, Instant usedAt, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.createdAt = createdAt;
        this.usedAt = usedAt;
        if (codeHash == null || codeHash.isBlank()) {
            throw new ValidationException("Recovery code hash must not be blank");
        }
        this.codeHash = codeHash;
    }

    public static RecoveryCode create(UUID id, UUID accountId, String codeHash, Instant now) {
        return new RecoveryCode(id, accountId, codeHash, null, now);
    }

    public static RecoveryCode reconstitute(UUID id, UUID accountId, String codeHash, Instant usedAt,
                                            Instant createdAt) {
        return new RecoveryCode(id, accountId, codeHash, usedAt, createdAt);
    }

    public void consume(Instant now) {
        if (usedAt != null) {
            throw new ValidationException("Recovery code has already been used");
        }
        this.usedAt = now;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
