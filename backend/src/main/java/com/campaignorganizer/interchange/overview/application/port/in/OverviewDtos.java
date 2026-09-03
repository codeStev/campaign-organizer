package com.campaignorganizer.interchange.overview.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read models of the world overview stats (FR-62). */
public final class OverviewDtos {

    private OverviewDtos() {
    }

    /** One entry in the recently-edited feed. */
    public record RecentlyEditedArticle(UUID articleId, String title, Instant updatedAt) {
    }

    public record WorldOverviewStats(
            int articleCount,
            int sessionsRunCount,
            List<RecentlyEditedArticle> recentlyEdited) {
    }
}
