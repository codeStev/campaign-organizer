package com.campaignorganizer.characters.application.statblock.port.published;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published read model for a statblock. */
public record StatblockView(
        UUID id,
        UUID worldId,
        UUID categoryId,
        UUID articleId,
        UUID campaignId,
        UUID worldTemplateId,
        UUID globalTemplateId,
        String name,
        Map<String, Object> stats,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
