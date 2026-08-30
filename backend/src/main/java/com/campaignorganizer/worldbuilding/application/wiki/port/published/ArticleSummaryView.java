package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for an article listing (no body, to keep listings light). */
public record ArticleSummaryView(
        UUID id,
        UUID worldId,
        UUID categoryId,
        UUID parentArticleId,
        String title,
        String slug,
        String template,
        Instant createdAt,
        Instant updatedAt) {
}
