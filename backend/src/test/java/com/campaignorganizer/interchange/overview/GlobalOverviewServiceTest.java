package com.campaignorganizer.interchange.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.interchange.overview.application.port.in.GlobalOverviewDtos.GlobalOverviewStats;
import com.campaignorganizer.interchange.overview.application.service.GlobalOverviewService;
import com.campaignorganizer.security.CurrentUserPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Account-wide landing page stats (issue #68) against mocked published ports. */
@ExtendWith(MockitoExtension.class)
class GlobalOverviewServiceTest {

    private final UUID accountId = UUID.randomUUID();

    // "Today" is fixed to 2026-03-03 for the upcoming-session boundary checks.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CurrentUserPort currentUser;
    @Mock
    private WorldQueryPort worlds;
    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private SessionQueryPort sessions;

    private GlobalOverviewService service;

    @BeforeEach
    void setUp() {
        service = new GlobalOverviewService(currentUser, worlds, campaigns, sessions, clock);
        when(currentUser.currentAccountId()).thenReturn(accountId);
    }

    @Test
    void accountWithNoWorldsGetsEmptyLists() {
        when(worlds.findByOwner(accountId)).thenReturn(List.of());

        GlobalOverviewStats stats = service.overview();

        assertThat(stats.upcomingSessions()).isEmpty();
        assertThat(stats.campaignsNeedingAttention()).isEmpty();
    }

    @Test
    void upcomingSessionsSpanWorldsAndSortSoonestFirstExcludingPastAndUndated() {
        UUID worldAId = UUID.randomUUID();
        UUID worldBId = UUID.randomUUID();
        UUID campaignAId = UUID.randomUUID();
        UUID campaignBId = UUID.randomUUID();
        when(worlds.findByOwner(accountId)).thenReturn(List.of(
                world(worldAId, "World A"), world(worldBId, "World B")));
        when(campaigns.findByWorld(worldAId)).thenReturn(List.of(campaign(campaignAId, "Chronicle A")));
        when(campaigns.findByWorld(worldBId)).thenReturn(List.of(campaign(campaignBId, "Chronicle B")));
        when(sessions.findOrdered(campaignAId)).thenReturn(List.of(
                session("Past", LocalDate.parse("2026-03-01")),
                session("Far off", LocalDate.parse("2026-04-01")),
                session("Undated", null)));
        when(sessions.findOrdered(campaignBId)).thenReturn(List.of(
                session("Nearest", LocalDate.parse("2026-03-05"))));

        GlobalOverviewStats stats = service.overview();

        assertThat(stats.upcomingSessions()).extracting("title").containsExactly("Nearest", "Far off");
        assertThat(stats.upcomingSessions().get(0).worldName()).isEqualTo("World B");
        assertThat(stats.upcomingSessions().get(0).campaignName()).isEqualTo("Chronicle B");
    }

    @Test
    void plannedOrActiveCampaignWithNothingScheduledNeedsAttention() {
        UUID worldId = UUID.randomUUID();
        UUID plannedId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();
        when(worlds.findByOwner(accountId)).thenReturn(List.of(world(worldId, "World")));
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(
                campaign(plannedId, "Planned campaign", CampaignStatus.PLANNED),
                campaign(activeId, "Active campaign", CampaignStatus.ACTIVE)));
        when(sessions.findOrdered(plannedId)).thenReturn(List.of());
        when(sessions.findOrdered(activeId)).thenReturn(List.of());

        GlobalOverviewStats stats = service.overview();

        assertThat(stats.campaignsNeedingAttention()).extracting("campaignName")
                .containsExactlyInAnyOrder("Planned campaign", "Active campaign");
    }

    @Test
    void onHiatusOrCompletedCampaignWithNothingScheduledDoesNotNeedAttention() {
        UUID worldId = UUID.randomUUID();
        UUID hiatusId = UUID.randomUUID();
        UUID completedId = UUID.randomUUID();
        when(worlds.findByOwner(accountId)).thenReturn(List.of(world(worldId, "World")));
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(
                campaign(hiatusId, "On hiatus", CampaignStatus.ON_HIATUS),
                campaign(completedId, "Completed", CampaignStatus.COMPLETED)));
        when(sessions.findOrdered(hiatusId)).thenReturn(List.of());
        when(sessions.findOrdered(completedId)).thenReturn(List.of());

        GlobalOverviewStats stats = service.overview();

        assertThat(stats.campaignsNeedingAttention()).isEmpty();
    }

    @Test
    void activeCampaignWithAnUpcomingSessionDoesNotNeedAttention() {
        UUID worldId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        when(worlds.findByOwner(accountId)).thenReturn(List.of(world(worldId, "World")));
        when(campaigns.findByWorld(worldId)).thenReturn(List.of(
                campaign(campaignId, "Active campaign", CampaignStatus.ACTIVE)));
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(session("Next", LocalDate.parse("2026-03-10"))));

        GlobalOverviewStats stats = service.overview();

        assertThat(stats.campaignsNeedingAttention()).isEmpty();
    }

    private WorldView world(UUID id, String name) {
        return new WorldView(id, name, null, Map.of(), false, accountId, Instant.EPOCH, Instant.EPOCH);
    }

    private CampaignView campaign(UUID id, String name) {
        return campaign(id, name, CampaignStatus.ACTIVE);
    }

    private CampaignView campaign(UUID id, String name, CampaignStatus status) {
        return new CampaignView(id, UUID.randomUUID(), name, null, null, status, null, null, Instant.EPOCH,
                Instant.EPOCH);
    }

    private SessionView session(String title, LocalDate date) {
        return new SessionView(UUID.randomUUID(), UUID.randomUUID(), title, null, date, null, null, Instant.EPOCH,
                Instant.EPOCH);
    }
}
