package com.campaignorganizer.interchange.tags.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Read models for the cross-entity browse-by-tag view (ADR-0083). */
public final class TagBrowseDtos {

    private TagBrowseDtos() {
    }

    public record ArticleSummary(UUID id, UUID worldId, UUID categoryId, UUID parentArticleId,
                                 String title, String slug, String template, Instant createdAt,
                                 Instant updatedAt) {
    }

    public record StatblockSummary(UUID id, UUID worldId, UUID articleId, UUID campaignId,
                                   UUID templateId, String name, Map<String, Object> stats,
                                   String notes, Instant createdAt, Instant updatedAt) {
    }

    public record TagBrowseResult(String tag, List<ArticleSummary> articles,
                                  List<StatblockSummary> statblocks) {
    }
}
