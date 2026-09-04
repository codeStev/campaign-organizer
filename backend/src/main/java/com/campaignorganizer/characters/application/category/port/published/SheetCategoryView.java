package com.campaignorganizer.characters.application.category.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a shared sheet/statblock/document/template category. */
public record SheetCategoryView(
        UUID id,
        UUID worldId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
