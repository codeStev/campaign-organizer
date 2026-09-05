package com.campaignorganizer.interchange.overview.application.port.in;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read models of the world overview stats (FR-62, ADR-0102, ADR-0103). */
public final class OverviewDtos {

    private OverviewDtos() {
    }

    /** One entry in the recently-edited feed. */
    public record RecentlyEditedArticle(UUID articleId, String title, Instant updatedAt) {
    }

    /** The single nearest upcoming session across every campaign in the world. */
    public record NextSessionSummary(
            UUID sessionId,
            UUID campaignId,
            String campaignName,
            String title,
            LocalDate date,
            Integer sessionNumber) {
    }

    /** A not-yet-full clock, world-scoped summary. */
    public record ClockSummary(
            UUID clockId,
            UUID campaignId,
            String campaignName,
            String title,
            int filledSegments,
            int totalSegments) {
    }

    /** An open (unresolved) loose thread, world-scoped summary. */
    public record LooseThreadSummary(
            UUID threadId,
            UUID campaignId,
            String campaignName,
            String text) {
    }

    /** One dated session, world-scoped, for the session calendar (ADR-0107).
     * {@code campaignColor} is the raw persisted value (nullable) — the
     * frontend resolves an uncolored campaign's fallback color itself. */
    public record ScheduledSessionSummary(
            UUID sessionId,
            UUID campaignId,
            String campaignName,
            String campaignColor,
            String title,
            Integer sessionNumber,
            LocalDate date) {
    }

    public record WorldOverviewStats(
            int articleCount,
            int sessionsRunCount,
            List<RecentlyEditedArticle> recentlyEdited,
            NextSessionSummary nextSession,
            List<ClockSummary> openClocks,
            List<LooseThreadSummary> openLooseThreads,
            List<ScheduledSessionSummary> scheduledSessions) {
    }
}
