package com.campaignorganizer.campaign.domain.beatkind;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A GM-defined, world-scoped beat kind (e.g. "Combat", "Reveal") — aggregate root. See ADR-0101. */
public final class BeatKind {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private String color;
    private final Instant createdAt;
    private Instant updatedAt;

    private BeatKind(UUID id, UUID worldId, String name, String color, Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, color);
    }

    public static BeatKind create(UUID id, UUID worldId, String name, String color, Instant now) {
        return new BeatKind(id, worldId, name, color, now, now);
    }

    public static BeatKind reconstitute(UUID id, UUID worldId, String name, String color,
                                        Instant createdAt, Instant updatedAt) {
        return new BeatKind(id, worldId, name, color, createdAt, updatedAt);
    }

    public void update(String name, String color, Instant now) {
        apply(name, color);
        this.updatedAt = now;
    }

    private void apply(String name, String color) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Beat kind name must not be blank");
        }
        this.name = name;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
