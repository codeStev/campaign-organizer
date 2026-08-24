package com.campaignorganizer.interchange.export.application.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Old-id to new-id map built up during backup import (ADR-0061): every
 * entity in a bundle is assigned a fresh id up front (pass 1), so every
 * reference to it — whether the referencing entity comes before or after it
 * in the bundle — can already be resolved when that entity is persisted
 * (pass 2).
 */
final class IdRemap {

    private final Map<UUID, UUID> map = new HashMap<>();

    /** Assigns a fresh id for {@code oldId}, or returns the one already assigned. */
    void assign(UUID oldId) {
        map.computeIfAbsent(oldId, id -> UUID.randomUUID());
    }

    /** The new id for a reference that must exist (assigned in pass 1). */
    UUID get(UUID oldId) {
        UUID newId = map.get(oldId);
        if (newId == null) {
            throw new IllegalStateException("Id " + oldId + " was not assigned during import pass 1");
        }
        return newId;
    }

    /** The new id for an optional reference, or null if {@code oldId} is null or unresolved. */
    UUID getOrNull(UUID oldId) {
        return oldId == null ? null : map.get(oldId);
    }
}
