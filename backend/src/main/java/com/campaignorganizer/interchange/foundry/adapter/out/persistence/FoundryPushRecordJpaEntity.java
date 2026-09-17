package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a push-tracking row (maps {@code foundry_pushed_documents}). */
@Entity
@Table(name = "foundry_pushed_documents")
public class FoundryPushRecordJpaEntity {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "entity_type", nullable = false, length = 20, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private UUID entityId;

    @Column(name = "foundry_document_id", nullable = false, length = 16)
    private String foundryDocumentId;

    @Column(name = "pushed_at", nullable = false)
    private Instant pushedAt;

    protected FoundryPushRecordJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getFoundryDocumentId() {
        return foundryDocumentId;
    }

    public void setFoundryDocumentId(String foundryDocumentId) {
        this.foundryDocumentId = foundryDocumentId;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(Instant pushedAt) {
        this.pushedAt = pushedAt;
    }
}
