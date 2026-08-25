package com.campaignorganizer.interchange.usage.application.port.in;

import java.util.List;
import java.util.UUID;

/** Read models of the consistency report (FR-43). */
public final class ConsistencyDtos {

    private ConsistencyDtos() {
    }

    /** A {@code [[wiki-link]]} whose target resolves to no article. */
    public record BrokenLink(String sourceType, UUID sourceId, String sourceLabel, String target) {
    }

    /** An article flagged by one of the report's reference checks. */
    public record ArticleIssue(UUID articleId, String title) {
    }

    public record ConsistencyReport(
            List<BrokenLink> brokenLinks,
            List<ArticleIssue> orphanedArticles,
            List<ArticleIssue> unreferencedByCampaigns) {
    }
}
