package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for an article revision snapshot. */
public record ArticleRevisionView(
        UUID id,
        UUID articleId,
        String title,
        String slug,
        String template,
        String body,
        Instant createdAt) {
}
