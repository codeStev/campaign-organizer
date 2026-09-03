package com.campaignorganizer.interchange.overview.application.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.interchange.overview.application.port.in.GetWorldOverviewUseCase;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.RecentlyEditedArticle;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.WorldOverviewStats;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-world stats strip (FR-62): article count, sessions-run count, and a
 * recently-edited feed. Pure read composition over existing published
 * ports — no new aggregate, no persisted state (ADR-0102).
 */
@Service
public class WorldOverviewService implements GetWorldOverviewUseCase {

    private static final int RECENTLY_EDITED_LIMIT = 5;

    private final WorldQueryPort worlds;
    private final ArticleQueryPort articles;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final Clock clock;

    public WorldOverviewService(WorldQueryPort worlds, ArticleQueryPort articles,
                                CampaignQueryPort campaigns, SessionQueryPort sessions, Clock clock) {
        this.worlds = worlds;
        this.articles = articles;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public WorldOverviewStats overview(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
        List<ArticleView> worldArticles = articles.findByWorld(worldId);

        int sessionsRun = 0;
        LocalDate today = LocalDate.now(clock);
        for (CampaignView c : campaigns.findByWorld(worldId)) {
            for (SessionView s : sessions.findOrdered(c.id())) {
                if (s.date() != null && !s.date().isAfter(today)) {
                    sessionsRun++;
                }
            }
        }

        List<RecentlyEditedArticle> recentlyEdited = worldArticles.stream()
                .sorted(Comparator.comparing(ArticleView::updatedAt).reversed())
                .limit(RECENTLY_EDITED_LIMIT)
                .map(a -> new RecentlyEditedArticle(a.id(), a.title(), a.updatedAt()))
                .toList();

        return new WorldOverviewStats(worldArticles.size(), sessionsRun, recentlyEdited);
    }
}
