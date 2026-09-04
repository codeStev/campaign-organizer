package com.campaignorganizer.tables.application.category.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a shared table/deck category. */
public record TableDeckCategoryView(
        UUID id,
        UUID worldId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
