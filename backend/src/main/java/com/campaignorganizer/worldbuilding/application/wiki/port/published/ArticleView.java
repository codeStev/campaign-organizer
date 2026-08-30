package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a full article (raw body; auto-linked HTML is rendered separately). */
public record ArticleView(
        UUID id,
        UUID worldId,
        UUID categoryId,
        UUID parentArticleId,
        String title,
        String slug,
        String template,
        String body,
        Instant createdAt,
        Instant updatedAt) {
}
