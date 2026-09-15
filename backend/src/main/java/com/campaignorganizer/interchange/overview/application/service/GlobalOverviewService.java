package com.campaignorganizer.interchange.overview.application.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.overview.application.port.in.GetGlobalOverviewUseCase;
import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.CampaignNeedingAttention;
import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.GlobalOverviewStats;
import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.UpcomingSessionSummary;
import com.campaignorganizer.security.CurrentUserPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account-wide landing page data (issue #68): every upcoming session across
 * every world/campaign the account owns, soonest-first, plus campaigns
 * needing attention (issue #64: PLANNED/ACTIVE with nothing scheduled).
 * Pure read composition over existing published ports, one level above
 * {@link WorldOverviewService} (world-scoped) and
 * {@link CampaignOverviewService} (campaign-scoped) in the same family.
 */
@Service
public class GlobalOverviewService implements GetGlobalOverviewUseCase {

    private final CurrentUserPort currentUser;
    private final WorldQueryPort worlds;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final Clock clock;

    public GlobalOverviewService(CurrentUserPort currentUser, WorldQueryPort worlds, CampaignQueryPort campaigns,
                                 SessionQueryPort sessions, Clock clock) {
        this.currentUser = currentUser;
        this.worlds = worlds;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalOverviewStats overview() {
        LocalDate today = LocalDate.now(clock);
        List<UpcomingSessionSummary> upcomingSessions = new ArrayList<>();
        List<CampaignNeedingAttention> campaignsNeedingAttention = new ArrayList<>();

        for (WorldView world : worlds.findByOwner(currentUser.currentAccountId())) {
            for (CampaignView campaign : campaigns.findByWorld(world.id())) {
                boolean hasUpcomingSession = false;
                for (SessionView session : sessions.findOrdered(campaign.id())) {
                    if (session.date() == null || !session.date().isAfter(today)) {
                        continue;
                    }
                    hasUpcomingSession = true;
                    upcomingSessions.add(new UpcomingSessionSummary(session.id(), world.id(), world.name(),
                            campaign.id(), campaign.name(), campaign.color(), session.title(),
                            session.sessionNumber(), session.date()));
                }
                if (!hasUpcomingSession && needsAttention(campaign.status())) {
                    campaignsNeedingAttention.add(new CampaignNeedingAttention(world.id(), world.name(),
                            campaign.id(), campaign.name(), campaign.status()));
                }
            }
        }

        upcomingSessions = upcomingSessions.stream().sorted(Comparator.comparing(UpcomingSessionSummary::date))
                .toList();

        return new GlobalOverviewStats(upcomingSessions, campaignsNeedingAttention);
    }

    private boolean needsAttention(CampaignStatus status) {
        return status == CampaignStatus.PLANNED || status == CampaignStatus.ACTIVE;
    }
}
