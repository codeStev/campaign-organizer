package com.campaignorganizer.campaign.domain.campaign;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A campaign: a run of play within a world, holding sessions and arcs (aggregate root). */
public final class Campaign {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private String description;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private Campaign(UUID id, UUID worldId, String name, String description, String notes,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, description, notes);
    }

    public static Campaign create(UUID id, UUID worldId, String name, String description, String notes,
                                  Instant now) {
        return new Campaign(id, worldId, name, description, notes, now, now);
    }

    public static Campaign reconstitute(UUID id, UUID worldId, String name, String description,
                                        String notes, Instant createdAt, Instant updatedAt) {
        return new Campaign(id, worldId, name, description, notes, createdAt, updatedAt);
    }

    public void update(String name, String description, String notes, Instant now) {
        apply(name, description, notes);
        this.updatedAt = now;
    }

    private void apply(String name, String description, String notes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Campaign name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.notes = notes;
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

    public String getDescription() {
        return description;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
