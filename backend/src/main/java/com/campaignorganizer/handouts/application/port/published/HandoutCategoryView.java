package com.campaignorganizer.handouts.application.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a handout category. */
public record HandoutCategoryView(
        UUID id,
        UUID worldId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
