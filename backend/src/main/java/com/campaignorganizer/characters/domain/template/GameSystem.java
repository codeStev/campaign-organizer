package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A game system (D&D 5e, Pirate Borg, ...) — a real, top-level, world-
 * independent entity (aggregate root) rather than a free-text label, so
 * system-level metadata (e.g. rule references) has somewhere stable to
 * attach later. See ADR-0094, ADR-0095.
 */
public final class GameSystem {

    private final UUID id;
    private String name;
    private String tagline;
    private String color;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private GameSystem(UUID id, String name, String tagline, String color, String notes, Instant createdAt,
                       Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, tagline, color, notes);
    }

    public static GameSystem create(UUID id, String name, String tagline, String color, String notes,
                                    Instant now) {
        return new GameSystem(id, name, tagline, color, notes, now, now);
    }

    public static GameSystem reconstitute(UUID id, String name, String tagline, String color, String notes,
                                          Instant createdAt, Instant updatedAt) {
        return new GameSystem(id, name, tagline, color, notes, createdAt, updatedAt);
    }

    public void update(String name, String tagline, String color, String notes, Instant now) {
        apply(name, tagline, color, notes);
        this.updatedAt = now;
    }

    private void apply(String name, String tagline, String color, String notes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Game system name must not be blank");
        }
        this.name = name;
        this.tagline = tagline;
        this.color = color;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTagline() {
        return tagline;
    }

    public String getColor() {
        return color;
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
