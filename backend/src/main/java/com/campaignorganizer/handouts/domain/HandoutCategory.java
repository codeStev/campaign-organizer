package com.campaignorganizer.handouts.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A hierarchical grouping for handouts within a world (aggregate root, ADR-0105). */
public final class HandoutCategory {

    private final UUID id;
    private final UUID worldId;
    private UUID parentId;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    private HandoutCategory(UUID id, UUID worldId, UUID parentId, String name, Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(parentId, name);
    }

    public static HandoutCategory create(UUID id, UUID worldId, UUID parentId, String name, Instant now) {
        return new HandoutCategory(id, worldId, parentId, name, now, now);
    }

    public static HandoutCategory reconstitute(UUID id, UUID worldId, UUID parentId, String name,
                                        Instant createdAt, Instant updatedAt) {
        return new HandoutCategory(id, worldId, parentId, name, createdAt, updatedAt);
    }

    public void update(UUID parentId, String name, Instant now) {
        apply(parentId, name);
        this.updatedAt = now;
    }

    private void apply(UUID parentId, String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Handout category name must not be blank");
        }
        this.parentId = parentId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
