package com.campaignorganizer.tables.domain.category;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A hierarchical grouping shared by roll tables and card decks within a
 * world (aggregate root, ADR-0105) — one taxonomy for both, since they
 * already live in one Tables & Decks screen.
 */
public final class TableDeckCategory {

    private final UUID id;
    private final UUID worldId;
    private UUID parentId;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    private TableDeckCategory(UUID id, UUID worldId, UUID parentId, String name, Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(parentId, name);
    }

    public static TableDeckCategory create(UUID id, UUID worldId, UUID parentId, String name, Instant now) {
        return new TableDeckCategory(id, worldId, parentId, name, now, now);
    }

    public static TableDeckCategory reconstitute(UUID id, UUID worldId, UUID parentId, String name,
                                        Instant createdAt, Instant updatedAt) {
        return new TableDeckCategory(id, worldId, parentId, name, createdAt, updatedAt);
    }

    public void update(UUID parentId, String name, Instant now) {
        apply(parentId, name);
        this.updatedAt = now;
    }

    private void apply(UUID parentId, String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Table/deck category name must not be blank");
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
