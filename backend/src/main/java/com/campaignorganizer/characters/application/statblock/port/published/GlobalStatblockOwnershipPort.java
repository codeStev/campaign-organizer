package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.UUID;

/**
 * One-time backfill hook (ADR-0109): when the first account registers and is
 * promoted to ADMIN, every pre-existing global statblock (predating any
 * account) is assigned to it.
 */
public interface GlobalStatblockOwnershipPort {

    void assignUnownedTo(UUID ownerId);
}
