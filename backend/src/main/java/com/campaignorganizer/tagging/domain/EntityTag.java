package com.campaignorganizer.tagging.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * One tag applied to one entity (aggregate root, FR-47/ADR-0083). Names are
 * folded to trimmed lowercase here — the single place this rule is applied —
 * so every caller (persistence, autocomplete, browse) already sees the
 * canonical form; there is no rename/merge screen to fix up duplicates later.
 */
public final class EntityTag {

    public static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private final UUID worldId;
    private final EntityType entityType;
    private final UUID entityId;
    private final String name;
    private final Instant createdAt;

    private EntityTag(UUID id, UUID worldId, EntityType entityType, UUID entityId, String name,
                      Instant createdAt) {
        this.id = id;
        this.worldId = worldId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.name = name;
        this.createdAt = createdAt;
    }

    /** Creates a new tag, normalizing and validating the raw name. */
    public static EntityTag create(UUID id, UUID worldId, EntityType entityType, UUID entityId,
                                   String rawName, Instant now) {
        return new EntityTag(id, worldId, entityType, entityId, normalize(rawName), now);
    }

    /** Rebuild an existing tag from storage (already-normalized data). */
    public static EntityTag reconstitute(UUID id, UUID worldId, EntityType entityType,
                                         UUID entityId, String name, Instant createdAt) {
        return new EntityTag(id, worldId, entityType, entityId, name, createdAt);
    }

    /** Normalizes a raw tag name the same way {@link #create} does, for lookups. */
    public static String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ValidationException("Tag name must not be blank");
        }
        String trimmed = rawName.strip().toLowerCase();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "Tag name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return trimmed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
