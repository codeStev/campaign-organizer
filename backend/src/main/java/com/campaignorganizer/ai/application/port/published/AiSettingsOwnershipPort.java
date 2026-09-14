package com.campaignorganizer.ai.application.port.published;

import java.util.UUID;

/**
 * One-time backfill hook (ADR-0109): when the first account registers and is
 * promoted to ADMIN, every pre-existing AI provider setting row (predating
 * any account) is assigned to it.
 */
public interface AiSettingsOwnershipPort {

    void assignUnownedTo(UUID ownerId);
}
