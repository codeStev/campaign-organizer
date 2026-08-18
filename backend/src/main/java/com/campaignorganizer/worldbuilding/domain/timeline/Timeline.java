package com.campaignorganizer.worldbuilding.domain.timeline;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A named sequence of dated events, optionally driven by a calendar (aggregate root). */
public final class Timeline {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private String description;
    private UUID calendarId;
    private final Instant createdAt;
    private Instant updatedAt;

    private Timeline(UUID id, UUID worldId, String name, String description, UUID calendarId,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, description, calendarId);
    }

    public static Timeline create(UUID id, UUID worldId, String name, String description,
                                  UUID calendarId, Instant now) {
        return new Timeline(id, worldId, name, description, calendarId, now, now);
    }

    public static Timeline reconstitute(UUID id, UUID worldId, String name, String description,
                                        UUID calendarId, Instant createdAt, Instant updatedAt) {
        return new Timeline(id, worldId, name, description, calendarId, createdAt, updatedAt);
    }

    public void update(String name, String description, UUID calendarId, Instant now) {
        apply(name, description, calendarId);
        this.updatedAt = now;
    }

    private void apply(String name, String description, UUID calendarId) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Timeline name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.calendarId = calendarId;
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

    public UUID getCalendarId() {
        return calendarId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
