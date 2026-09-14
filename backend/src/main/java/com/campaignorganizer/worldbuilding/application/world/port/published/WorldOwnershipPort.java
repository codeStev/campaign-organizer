package com.campaignorganizer.worldbuilding.application.world.port.published;

import java.util.UUID;

/**
 * One-time backfill hook (ADR-0109): when the first account registers and is
 * promoted to ADMIN, every pre-existing World (predating any account) is
 * assigned to it.
 */
public interface WorldOwnershipPort {

    void assignUnownedTo(UUID ownerId);
}
