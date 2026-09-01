package com.campaignorganizer.campaign.domain.player;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A player: part of the world's reusable pool, added to campaign rosters (aggregate root). */
public final class Player {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    private Player(UUID id, UUID worldId, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name);
    }

    public static Player create(UUID id, UUID worldId, String name, Instant now) {
        return new Player(id, worldId, name, now, now);
    }

    public static Player reconstitute(UUID id, UUID worldId, String name, Instant createdAt,
                                      Instant updatedAt) {
        return new Player(id, worldId, name, createdAt, updatedAt);
    }

    public void update(String name, Instant now) {
        apply(name);
        this.updatedAt = now;
    }

    private void apply(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Player name must not be blank");
        }
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
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
