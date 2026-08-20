package com.campaignorganizer.characters.application.statblock.port.published;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published read model for a statblock. */
public record StatblockView(
        UUID id,
        UUID worldId,
        UUID articleId,
        UUID campaignId,
        UUID templateId,
        String name,
        Map<String, Object> stats,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
