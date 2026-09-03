package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a map category. */
public record MapCategoryView(
        UUID id,
        UUID worldId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
