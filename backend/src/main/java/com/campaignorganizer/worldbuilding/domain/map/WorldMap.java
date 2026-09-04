package com.campaignorganizer.worldbuilding.domain.map;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** An uploaded map image for a world (aggregate root). */
public final class WorldMap {

    private final UUID id;
    private final UUID worldId;
    private UUID categoryId;
    private String name;
    private UUID mediaId;
    private final Instant createdAt;
    private Instant updatedAt;

    private WorldMap(UUID id, UUID worldId, UUID categoryId, String name, UUID mediaId, Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(categoryId, name, mediaId);
    }

    public static WorldMap create(UUID id, UUID worldId, String name, UUID mediaId, Instant now) {
        return new WorldMap(id, worldId, null, name, mediaId, now, now);
    }

    public static WorldMap reconstitute(UUID id, UUID worldId, UUID categoryId, String name, UUID mediaId,
                                        Instant createdAt, Instant updatedAt) {
        return new WorldMap(id, worldId, categoryId, name, mediaId, createdAt, updatedAt);
    }

    public void update(UUID categoryId, String name, UUID mediaId, Instant now) {
        apply(categoryId, name, mediaId);
        this.updatedAt = now;
    }

    private void apply(UUID categoryId, String name, UUID mediaId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Map name must not be blank");
        }
        if (mediaId == null) {
            throw new ValidationException("A map needs an image");
        }
        this.categoryId = categoryId;
        this.name = name;
        this.mediaId = mediaId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public UUID getMediaId() {
        return mediaId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
