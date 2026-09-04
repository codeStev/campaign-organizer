package com.campaignorganizer.worldbuilding.domain.world;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A world: the tenant root that all worldbuilding and campaign content hangs off (aggregate root). */
public final class World {

    private final UUID id;
    private String name;
    private String description;
    private Map<String, LayerStyle> layerStyles;
    /** Cosmetic-only sandbox/brainstorming flag (FR-60, ADR-0100) — no functional effect. */
    private boolean scratch;
    private final Instant createdAt;
    private Instant updatedAt;

    private World(UUID id, String name, String description, Map<String, LayerStyle> layerStyles,
                  boolean scratch, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.layerStyles = layerStyles == null ? new HashMap<>() : layerStyles;
        this.scratch = scratch;
        applyDetails(name, description);
    }

    public static World create(UUID id, String name, String description, boolean scratch, Instant now) {
        return new World(id, name, description, new HashMap<>(), scratch, now, now);
    }

    public static World reconstitute(UUID id, String name, String description,
                                     Map<String, LayerStyle> layerStyles, boolean scratch, Instant createdAt,
                                     Instant updatedAt) {
        return new World(id, name, description, layerStyles, scratch, createdAt, updatedAt);
    }

    public void update(String name, String description, boolean scratch, Instant now) {
        applyDetails(name, description);
        this.scratch = scratch;
        this.updatedAt = now;
    }

    public void replaceLayerStyles(Map<String, LayerStyle> layerStyles, Instant now) {
        this.layerStyles = layerStyles == null ? new HashMap<>() : layerStyles;
        this.updatedAt = now;
    }

    private void applyDetails(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("World name must not be blank");
        }
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, LayerStyle> getLayerStyles() {
        return layerStyles;
    }

    public boolean isScratch() {
        return scratch;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
