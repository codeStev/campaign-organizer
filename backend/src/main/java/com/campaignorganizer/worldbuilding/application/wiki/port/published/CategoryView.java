package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a category. */
public record CategoryView(
        UUID id,
        UUID worldId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
