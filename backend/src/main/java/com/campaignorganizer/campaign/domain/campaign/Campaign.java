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
    private CampaignStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Campaign(UUID id, UUID worldId, String name, String description, String notes,
                     CampaignStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, description, notes, status);
    }

    public static Campaign create(UUID id, UUID worldId, String name, String description, String notes,
                                  CampaignStatus status, Instant now) {
        return new Campaign(id, worldId, name, description, notes, status, now, now);
    }

    public static Campaign reconstitute(UUID id, UUID worldId, String name, String description,
                                        String notes, CampaignStatus status, Instant createdAt,
                                        Instant updatedAt) {
        return new Campaign(id, worldId, name, description, notes, status, createdAt, updatedAt);
    }

    public void update(String name, String description, String notes, CampaignStatus status, Instant now) {
        apply(name, description, notes, status);
        this.updatedAt = now;
    }

    private void apply(String name, String description, String notes, CampaignStatus status) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Campaign name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.notes = notes;
        this.status = status == null ? CampaignStatus.PLANNED : status;
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

    public CampaignStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
