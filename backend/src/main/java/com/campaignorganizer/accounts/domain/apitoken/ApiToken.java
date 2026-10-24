package com.campaignorganizer.accounts.domain.apitoken;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * A machine-client credential owned by one account (aggregate root, ADR-0119). The secret itself
 * is never part of the model, only its hash and a short display prefix.
 */
public final class ApiToken {

    static final int MAX_NAME_LENGTH = 100;
    /** Usage is recorded at most this often, so authenticating does not write on every request. */
    static final Duration LAST_USED_RESOLUTION = Duration.ofMinutes(5);

    private final UUID id;
    private final UUID accountId;
    private final int accountTokenVersion;
    private final String name;
    private final String tokenHash;
    private final String prefix;
    private final Set<ApiTokenScope> scopes;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;

    private ApiToken(UUID id, UUID accountId, int accountTokenVersion, String name, String tokenHash, String prefix,
                     Set<ApiTokenScope> scopes, Instant createdAt, Instant expiresAt, Instant lastUsedAt,
                     Instant revokedAt) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("API token name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("API token name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new ValidationException("API token hash must not be blank");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new ValidationException("API token needs at least one scope");
        }
        this.id = id;
        this.accountId = accountId;
        this.accountTokenVersion = accountTokenVersion;
        this.name = name;
        this.tokenHash = tokenHash;
        this.prefix = prefix;
        this.scopes = EnumSet.copyOf(scopes);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
    }

    public static ApiToken create(UUID id, UUID accountId, int accountTokenVersion, String name, String tokenHash, String prefix,
                                  Set<ApiTokenScope> scopes, Instant expiresAt, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new ValidationException("API token expiry must be in the future");
        }
        return new ApiToken(id, accountId, accountTokenVersion, name, tokenHash, prefix, scopes, now, expiresAt, null,
                null);
    }

    public static ApiToken reconstitute(UUID id, UUID accountId, int accountTokenVersion, String name, String tokenHash, String prefix,
                                        Set<ApiTokenScope> scopes, Instant createdAt, Instant expiresAt,
                                        Instant lastUsedAt, Instant revokedAt) {
        return new ApiToken(id, accountId, accountTokenVersion, name, tokenHash, prefix, scopes, createdAt,
                expiresAt, lastUsedAt, revokedAt);
    }

    public boolean isUsable(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    public boolean allows(ApiTokenScope scope) {
        return scopes.contains(scope);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
    }

    /** @return true if the stored last-used time changed and the token needs saving */
    public boolean recordUse(Instant now) {
        if (lastUsedAt != null && lastUsedAt.plus(LAST_USED_RESOLUTION).isAfter(now)) {
            return false;
        }
        this.lastUsedAt = now;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public int getAccountTokenVersion() {
        return accountTokenVersion;
    }

    public String getName() {
        return name;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getPrefix() {
        return prefix;
    }

    public Set<ApiTokenScope> getScopes() {
        return EnumSet.copyOf(scopes);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
