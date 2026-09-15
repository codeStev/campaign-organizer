package com.campaignorganizer.interchange.overview.application.port.in;

import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read models of the account-wide landing page stats (issue #68). */
public final class GlobalOverviewDtos {

    private GlobalOverviewDtos() {
    }

    /** One dated session, across every world/campaign the account owns. */
    public record UpcomingSessionSummary(
            UUID sessionId,
            UUID worldId,
            String worldName,
            UUID campaignId,
            String campaignName,
            String campaignColor,
            String title,
            Integer sessionNumber,
            LocalDate date) {
    }

    /** A PLANNED or ACTIVE campaign with nothing scheduled (issue #64). */
    public record CampaignNeedingAttention(
            UUID worldId,
            String worldName,
            UUID campaignId,
            String campaignName,
            CampaignStatus status) {
    }

    public record GlobalOverviewStats(
            List<UpcomingSessionSummary> upcomingSessions,
            List<CampaignNeedingAttention> campaignsNeedingAttention) {
    }
}
