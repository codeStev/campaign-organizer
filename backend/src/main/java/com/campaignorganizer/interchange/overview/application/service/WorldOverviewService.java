package com.campaignorganizer.interchange.overview.application.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.interchange.overview.application.port.in.GetWorldOverviewUseCase;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.ClockSummary;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.LooseThreadSummary;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.NextSessionSummary;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.RecentlyEditedArticle;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.WorldOverviewStats;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-world Overview screen data (FR-62): stats strip, next-session card,
 * Clocks widget, Loose Threads widget. Pure read composition over existing
 * published ports — no new aggregate, no persisted state (ADR-0102,
 * ADR-0103).
 */
@Service
public class WorldOverviewService implements GetWorldOverviewUseCase {

    private static final int RECENTLY_EDITED_LIMIT = 5;
    private static final int OPEN_CLOCKS_LIMIT = 8;
    private static final int OPEN_LOOSE_THREADS_LIMIT = 8;

    private final WorldQueryPort worlds;
    private final ArticleQueryPort articles;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ClockQueryPort clocks;
    private final LooseThreadQueryPort looseThreads;
    private final Clock clock;

    public WorldOverviewService(WorldQueryPort worlds, ArticleQueryPort articles,
                                CampaignQueryPort campaigns, SessionQueryPort sessions,
                                ClockQueryPort clocks, LooseThreadQueryPort looseThreads, Clock clock) {
        this.worlds = worlds;
        this.articles = articles;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.clocks = clocks;
        this.looseThreads = looseThreads;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public WorldOverviewStats overview(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
        List<ArticleView> worldArticles = articles.findByWorld(worldId);
        LocalDate today = LocalDate.now(clock);

        int sessionsRun = 0;
        NextSessionSummary nextSession = null;
        List<ClockSummary> openClocks = new ArrayList<>();
        List<OpenThread> openThreads = new ArrayList<>();

        for (CampaignView c : campaigns.findByWorld(worldId)) {
            for (SessionView s : sessions.findOrdered(c.id())) {
                if (s.date() == null) {
                    continue;
                }
                if (!s.date().isAfter(today)) {
                    sessionsRun++;
                } else if (nextSession == null || s.date().isBefore(nextSession.date())) {
                    nextSession = new NextSessionSummary(s.id(), c.id(), c.name(), s.title(), s.date(),
                            s.sessionNumber());
                }
            }
            for (ClockView clockView : clocks.findByCampaign(c.id())) {
                int filled = (int) clockView.segments().stream().filter(ClockSegmentView::filled).count();
                int total = clockView.segments().size();
                if (filled < total) {
                    openClocks.add(new ClockSummary(clockView.id(), c.id(), c.name(), clockView.title(),
                            filled, total));
                }
            }
            for (LooseThreadView thread : looseThreads.findByCampaign(c.id())) {
                if ("OPEN".equals(thread.status())) {
                    openThreads.add(new OpenThread(thread, c.id(), c.name()));
                }
            }
        }

        openClocks = openClocks.stream()
                .sorted(Comparator.<ClockSummary>comparingDouble(
                        s -> (double) s.filledSegments() / s.totalSegments()).reversed())
                .limit(OPEN_CLOCKS_LIMIT)
                .toList();

        List<LooseThreadSummary> openLooseThreads = openThreads.stream()
                .sorted(Comparator.comparing((OpenThread t) -> t.thread().createdAt()).reversed())
                .limit(OPEN_LOOSE_THREADS_LIMIT)
                .map(t -> new LooseThreadSummary(t.thread().id(), t.campaignId(), t.campaignName(),
                        t.thread().text()))
                .toList();

        List<RecentlyEditedArticle> recentlyEdited = worldArticles.stream()
                .sorted(Comparator.comparing(ArticleView::updatedAt).reversed())
                .limit(RECENTLY_EDITED_LIMIT)
                .map(a -> new RecentlyEditedArticle(a.id(), a.title(), a.updatedAt()))
                .toList();

        return new WorldOverviewStats(worldArticles.size(), sessionsRun, recentlyEdited, nextSession,
                openClocks, openLooseThreads);
    }

    /** A loose thread plus its campaign context, kept around only long enough to sort by createdAt. */
    private record OpenThread(LooseThreadView thread, UUID campaignId, String campaignName) {
    }
}
