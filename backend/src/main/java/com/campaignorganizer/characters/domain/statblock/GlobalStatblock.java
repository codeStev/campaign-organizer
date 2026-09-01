package com.campaignorganizer.characters.domain.statblock;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A reusable statblock instance shared across every world (aggregate root) —
 * game-system-scoped, imported (copied) into a specific campaign on demand
 * rather than referenced live. See ADR-0096.
 */
public final class GlobalStatblock {

    private final UUID id;
    private UUID systemId;
    private UUID globalTemplateId;
    private String name;
    private Map<String, Object> stats;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private GlobalStatblock(UUID id, UUID systemId, UUID globalTemplateId, String name,
                            Map<String, Object> stats, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(systemId, globalTemplateId, name, stats, notes);
    }

    public static GlobalStatblock create(UUID id, UUID systemId, UUID globalTemplateId, String name,
                                         Map<String, Object> stats, String notes, Instant now) {
        return new GlobalStatblock(id, systemId, globalTemplateId, name, stats, notes, now, now);
    }

    public static GlobalStatblock reconstitute(UUID id, UUID systemId, UUID globalTemplateId, String name,
                                               Map<String, Object> stats, String notes, Instant createdAt,
                                               Instant updatedAt) {
        return new GlobalStatblock(id, systemId, globalTemplateId, name, stats, notes, createdAt, updatedAt);
    }

    public void update(UUID systemId, UUID globalTemplateId, String name, Map<String, Object> stats,
                       String notes, Instant now) {
        apply(systemId, globalTemplateId, name, stats, notes);
        this.updatedAt = now;
    }

    private void apply(UUID systemId, UUID globalTemplateId, String name, Map<String, Object> stats,
                       String notes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Global statblock name must not be blank");
        }
        if (systemId == null) {
            throw new ValidationException("Global statblock system must not be null");
        }
        this.systemId = systemId;
        this.globalTemplateId = globalTemplateId;
        this.name = name;
        this.stats = stats == null ? new HashMap<>() : stats;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public UUID getGlobalTemplateId() {
        return globalTemplateId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getStats() {
        return stats;
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
