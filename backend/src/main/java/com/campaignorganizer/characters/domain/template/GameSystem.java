package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A game system (D&D 5e, Pirate Borg, ...) — a real, top-level, world-
 * independent entity (aggregate root) rather than a free-text label, so
 * system-level metadata (e.g. rule references) has somewhere stable to
 * attach later. See ADR-0094.
 */
public final class GameSystem {

    private final UUID id;
    private String name;
    private final Instant createdAt;
    private Instant updatedAt;

    private GameSystem(UUID id, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name);
    }

    public static GameSystem create(UUID id, String name, Instant now) {
        return new GameSystem(id, name, now, now);
    }

    public static GameSystem reconstitute(UUID id, String name, Instant createdAt, Instant updatedAt) {
        return new GameSystem(id, name, createdAt, updatedAt);
    }

    public void update(String name, Instant now) {
        apply(name);
        this.updatedAt = now;
    }

    private void apply(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Game system name must not be blank");
        }
        this.name = name;
    }

    public UUID getId() {
        return id;
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
