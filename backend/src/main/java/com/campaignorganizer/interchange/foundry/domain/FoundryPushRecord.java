package com.campaignorganizer.interchange.foundry.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the last successful push of one Campaign Organizer entity to Foundry
 * (ADR-0115) — one row per (world, entity type, entity id), so a re-push is
 * an idempotent upsert (same deterministic {@code foundryDocumentId} every
 * time) and the UI can show "last pushed at ...". Not yet written to by any
 * use case until article push (a later phase) exists; created now so the
 * schema lands with everything else in this feature's first migration.
 */
public final class FoundryPushRecord {

    private final UUID id;
    private final UUID worldId;
    private final FoundryEntityType entityType;
    private final UUID entityId;
    private String foundryDocumentId;
    private Instant pushedAt;

    private FoundryPushRecord(UUID id, UUID worldId, FoundryEntityType entityType, UUID entityId,
                              String foundryDocumentId, Instant pushedAt) {
        this.id = id;
        this.worldId = worldId;
        this.entityType = entityType;
        this.entityId = entityId;
        apply(foundryDocumentId, pushedAt);
    }

    public static FoundryPushRecord create(UUID id, UUID worldId, FoundryEntityType entityType, UUID entityId,
                                           String foundryDocumentId, Instant pushedAt) {
        return new FoundryPushRecord(id, worldId, entityType, entityId, foundryDocumentId, pushedAt);
    }

    public static FoundryPushRecord reconstitute(UUID id, UUID worldId, FoundryEntityType entityType,
                                                 UUID entityId, String foundryDocumentId, Instant pushedAt) {
        return new FoundryPushRecord(id, worldId, entityType, entityId, foundryDocumentId, pushedAt);
    }

    /** Re-push: the stable id never actually changes, but recorded again for clarity. */
    public void recordPush(String foundryDocumentId, Instant pushedAt) {
        apply(foundryDocumentId, pushedAt);
    }

    private void apply(String foundryDocumentId, Instant pushedAt) {
        if (foundryDocumentId == null || foundryDocumentId.isBlank()) {
            throw new ValidationException("Foundry document id must not be blank");
        }
        this.foundryDocumentId = foundryDocumentId;
        this.pushedAt = pushedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public FoundryEntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getFoundryDocumentId() {
        return foundryDocumentId;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }
}
