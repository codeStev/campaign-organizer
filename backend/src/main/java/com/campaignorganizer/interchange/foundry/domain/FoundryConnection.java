package com.campaignorganizer.interchange.foundry.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A world's connection to a self-hosted Foundry VTT relay (ADR-0115) — one per
 * world, keyed by worldId rather than its own id (aggregate root, but 1:1 with
 * the world it belongs to, same shape as {@code CalendarFeed}). The API key
 * is always already encrypted by the time it reaches this class — the domain
 * holds an opaque ciphertext string and never performs encryption/decryption
 * itself; that happens in the application layer, which owns the {@code
 * TextEncryptor} bean.
 */
public final class FoundryConnection {

    private final UUID worldId;
    private String relayBaseUrl;
    private String clientId;
    private String apiKeyEncrypted;
    private final Instant createdAt;
    private Instant updatedAt;

    private FoundryConnection(UUID worldId, String relayBaseUrl, String clientId, String apiKeyEncrypted,
                              Instant createdAt, Instant updatedAt) {
        this.worldId = worldId;
        this.createdAt = createdAt;
        apply(relayBaseUrl, clientId, apiKeyEncrypted);
        this.updatedAt = updatedAt;
    }

    public static FoundryConnection create(UUID worldId, String relayBaseUrl, String clientId,
                                           String apiKeyEncrypted, Instant now) {
        return new FoundryConnection(worldId, relayBaseUrl, clientId, apiKeyEncrypted, now, now);
    }

    public static FoundryConnection reconstitute(UUID worldId, String relayBaseUrl, String clientId,
                                                 String apiKeyEncrypted, Instant createdAt, Instant updatedAt) {
        return new FoundryConnection(worldId, relayBaseUrl, clientId, apiKeyEncrypted, createdAt, updatedAt);
    }

    /** Always takes a resolved ciphertext — "keep the existing key" is an application-layer decision. */
    public void update(String relayBaseUrl, String clientId, String apiKeyEncrypted, Instant now) {
        apply(relayBaseUrl, clientId, apiKeyEncrypted);
        this.updatedAt = now;
    }

    private void apply(String relayBaseUrl, String clientId, String apiKeyEncrypted) {
        if (relayBaseUrl == null || relayBaseUrl.isBlank()) {
            throw new ValidationException("Relay base URL must not be blank");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new ValidationException("Foundry client id must not be blank");
        }
        if (apiKeyEncrypted == null || apiKeyEncrypted.isBlank()) {
            throw new ValidationException("Foundry API key must not be blank");
        }
        this.relayBaseUrl = relayBaseUrl;
        this.clientId = clientId;
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getRelayBaseUrl() {
        return relayBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
