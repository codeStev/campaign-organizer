package com.campaignorganizer.characters.application.template.port.published;

import java.util.UUID;

/**
 * One-time backfill hook (ADR-0109): when the first account registers and is
 * promoted to ADMIN, every pre-existing game system (predating any account)
 * is assigned to it.
 */
public interface GameSystemOwnershipPort {

    void assignUnownedTo(UUID ownerId);
}
