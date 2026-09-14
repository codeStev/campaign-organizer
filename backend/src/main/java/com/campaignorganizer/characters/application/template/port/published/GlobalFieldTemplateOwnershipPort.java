package com.campaignorganizer.characters.application.template.port.published;

import java.util.UUID;

/**
 * One-time backfill hook (ADR-0109): when the first account registers and is
 * promoted to ADMIN, every pre-existing global field template (predating any
 * account) is assigned to it.
 */
public interface GlobalFieldTemplateOwnershipPort {

    void assignUnownedTo(UUID ownerId);
}
