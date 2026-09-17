package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a world's Foundry connection (maps {@code foundry_connections}). */
@Entity
@Table(name = "foundry_connections")
public class FoundryConnectionJpaEntity {

    @Id
    @Column(name = "world_id")
    private UUID worldId;

    @Column(name = "relay_base_url", nullable = false)
    private String relayBaseUrl;

    @Column(name = "client_id", nullable = false)
    private String clientId;

    @Column(name = "api_key_encrypted", nullable = false, length = 500)
    private String apiKeyEncrypted;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FoundryConnectionJpaEntity() {
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getRelayBaseUrl() {
        return relayBaseUrl;
    }

    public void setRelayBaseUrl(String relayBaseUrl) {
        this.relayBaseUrl = relayBaseUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApiKeyEncrypted() {
        return apiKeyEncrypted;
    }

    public void setApiKeyEncrypted(String apiKeyEncrypted) {
        this.apiKeyEncrypted = apiKeyEncrypted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
