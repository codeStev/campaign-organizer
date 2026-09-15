package com.campaignorganizer.interchange.overview.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read models of the per-campaign dashboard stats (issue #67). */
public final class CampaignOverviewDtos {

    private CampaignOverviewDtos() {
    }

    /** The single nearest upcoming session in this campaign. */
    public record CampaignNextSessionSummary(
            UUID sessionId,
            String title,
            LocalDate date,
            Integer sessionNumber) {
    }

    /** A not-yet-full clock, with a prep hint for what's next. */
    public record ClockPrepHint(
            UUID clockId,
            String title,
            int filledSegments,
            int totalSegments,
            String nextUnfilledSegmentTitle) {
    }

    /** An open (unresolved) loose thread. */
    public record CampaignLooseThreadSummary(UUID threadId, String text) {
    }

    /** A not-done beat belonging to an ACTIVE-status arc. */
    public record OpenBeatSummary(UUID beatId, UUID arcId, String arcTitle, String beatTitle) {
    }

    /** A not-done todo attached to the next session. */
    public record CampaignTodoSummary(UUID todoId, String text) {
    }

    public record CampaignOverviewStats(
            CampaignNextSessionSummary nextSession,
            List<ClockPrepHint> openClocksNearFilling,
            List<CampaignLooseThreadSummary> openLooseThreads,
            List<OpenBeatSummary> openBeatsInActiveArcs,
            List<CampaignTodoSummary> nextSessionTodos) {
    }
}
