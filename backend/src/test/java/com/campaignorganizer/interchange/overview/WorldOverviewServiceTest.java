package com.campaignorganizer.interchange.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.overview.application.port.in.OverviewDtos.WorldOverviewStats;
import com.campaignorganizer.interchange.overview.application.service.WorldOverviewService;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** World overview stats (FR-62, ADR-0102, ADR-0103) against mocked published ports. */
@ExtendWith(MockitoExtension.class)
class WorldOverviewServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    // "Today" is fixed to 2026-03-03 for the sessions-run boundary checks.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private WorldQueryPort worlds;
    @Mock
    private ArticleQueryPort articles;
    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private SessionQueryPort sessions;
    @Mock
    private ClockQueryPort clocks;
    @Mock
    private LooseThreadQueryPort looseThreads;

    private WorldOverviewService service;

    @BeforeEach
    void setUp() {
        service = new WorldOverviewService(worlds, articles, campaigns, sessions, clocks, looseThreads, clock);
        when(worlds.exists(worldId)).thenReturn(true);
    }

    @Test
    void unknownWorldIsNotFound() {
        when(worlds.exists(worldId)).thenReturn(false);
        assertThatThrownBy(() -> service.overview(worldId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void countsArticlesAndOrdersRecentlyEditedNewestFirst() {
        ArticleView older = article("Older", Instant.parse("2026-01-01T00:00:00Z"));
        ArticleView newer = article("Newer", Instant.parse("2026-02-01T00:00:00Z"));
        when(articles.findByWorld(worldId)).thenReturn(List.of(older, newer));
        when(campaigns.findByWorld(worldId)).thenReturn(List.of());

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.articleCount()).isEqualTo(2);
        assertThat(stats.recentlyEdited()).extracting("title").containsExactly("Newer", "Older");
    }

    @Test
    void recentlyEditedIsCappedAtFive() {
        List<ArticleView> six = List.of(
                article("A", Instant.parse("2026-01-01T00:00:00Z")),
                article("B", Instant.parse("2026-01-02T00:00:00Z")),
                article("C", Instant.parse("2026-01-03T00:00:00Z")),
                article("D", Instant.parse("2026-01-04T00:00:00Z")),
                article("E", Instant.parse("2026-01-05T00:00:00Z")),
                article("F", Instant.parse("2026-01-06T00:00:00Z")));
        when(articles.findByWorld(worldId)).thenReturn(six);
        when(campaigns.findByWorld(worldId)).thenReturn(List.of());

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.articleCount()).isEqualTo(6);
        assertThat(stats.recentlyEdited()).hasSize(5);
        assertThat(stats.recentlyEdited()).extracting("title")
                .containsExactly("F", "E", "D", "C", "B");
    }

    @Test
    void countsOnlySessionsDatedTodayOrEarlierAcrossEveryCampaign() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());
        CampaignView campaign = new CampaignView(campaignId, worldId, "Chronicle", null, null,
                CampaignStatus.ACTIVE, null, null, Instant.EPOCH, Instant.EPOCH);
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(
                session(LocalDate.parse("2026-03-01")), // run
                session(LocalDate.parse("2026-03-03")), // run (today)
                session(LocalDate.parse("2026-03-10")), // future — not run
                session(null))); // never scheduled — not run

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.sessionsRunCount()).isEqualTo(2);
    }

    @Test
    void nextSessionIsNearestFutureSessionAcrossCampaigns() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());
        UUID otherCampaignId = UUID.randomUUID();
        CampaignView campaign = campaign(campaignId, "Chronicle");
        CampaignView otherCampaign = campaign(otherCampaignId, "Side Quest");
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign, otherCampaign));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(
                session(campaignId, "Far off", LocalDate.parse("2026-04-01")),
                session(campaignId, "Today, not next", LocalDate.parse("2026-03-03"))));
        when(sessions.findOrdered(otherCampaignId)).thenReturn(List.of(
                session(otherCampaignId, "Nearest", LocalDate.parse("2026-03-05"))));

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.nextSession()).isNotNull();
        assertThat(stats.nextSession().title()).isEqualTo("Nearest");
        assertThat(stats.nextSession().campaignName()).isEqualTo("Side Quest");
    }

    @Test
    void nextSessionIsNullWhenNothingScheduled() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign(campaignId, "Chronicle")));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of());

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.nextSession()).isNull();
    }

    @Test
    void openClocksExcludesFullOnesAndSortsMostFilledFirst() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign(campaignId, "Chronicle")));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of());
        when(clocks.findByCampaign(campaignId)).thenReturn(List.of(
                clockView("Nearly there", 5, 6),
                clockView("Just started", 1, 6),
                clockView("Complete", 6, 6)));

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.openClocks()).extracting("title").containsExactly("Nearly there", "Just started");
    }

    @Test
    void openLooseThreadsExcludesResolvedAndSortsNewestFirst() {
        when(articles.findByWorld(worldId)).thenReturn(List.of());
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(campaign(campaignId, "Chronicle")));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of());
        when(looseThreads.findByCampaign(campaignId)).thenReturn(List.of(
                thread("Older open thread", "OPEN", Instant.parse("2026-01-01T00:00:00Z")),
                thread("Newer open thread", "OPEN", Instant.parse("2026-02-01T00:00:00Z")),
                thread("Resolved thread", "RESOLVED", Instant.parse("2026-03-01T00:00:00Z"))));

        WorldOverviewStats stats = service.overview(worldId);

        assertThat(stats.openLooseThreads()).extracting("text")
                .containsExactly("Newer open thread", "Older open thread");
    }

    private CampaignView campaign(UUID id, String name) {
        return new CampaignView(id, worldId, name, null, null, CampaignStatus.ACTIVE, null, null,
                Instant.EPOCH, Instant.EPOCH);
    }

    private SessionView session(UUID forCampaignId, String title, LocalDate date) {
        return new SessionView(UUID.randomUUID(), forCampaignId, title, null, date, null, null,
                Instant.EPOCH, Instant.EPOCH);
    }

    private ClockView clockView(String title, int filled, int total) {
        List<ClockSegmentView> segments = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            segments.add(new ClockSegmentView(i < filled, null, null));
        }
        return new ClockView(UUID.randomUUID(), campaignId, title, null, segments, 0, Instant.EPOCH,
                Instant.EPOCH);
    }

    private LooseThreadView thread(String text, String status, Instant createdAt) {
        return new LooseThreadView(UUID.randomUUID(), UUID.randomUUID(), campaignId, text, status,
                createdAt, createdAt);
    }

    private ArticleView article(String title, Instant updatedAt) {
        return new ArticleView(UUID.randomUUID(), worldId, null, null, title, title.toLowerCase(),
                null, null, updatedAt, updatedAt);
    }

    private SessionView session(LocalDate date) {
        return new SessionView(UUID.randomUUID(), campaignId, "Session", null, date, null, null,
                Instant.EPOCH, Instant.EPOCH);
    }
}
