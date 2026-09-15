package com.campaignorganizer.interchange.overview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.application.todo.port.published.TodoQueryPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignOverviewStats;
import com.campaignorganizer.interchange.overview.application.service.CampaignOverviewService;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Per-campaign dashboard stats (issue #67) against mocked published ports. */
@ExtendWith(MockitoExtension.class)
class CampaignOverviewServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();

    // "Today" is fixed to 2026-03-03 for the next-session boundary checks.
    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CampaignQueryPort campaigns;
    @Mock
    private SessionQueryPort sessions;
    @Mock
    private ClockQueryPort clocks;
    @Mock
    private LooseThreadQueryPort looseThreads;
    @Mock
    private ArcQueryPort arcs;
    @Mock
    private ArcBeatQueryPort beats;
    @Mock
    private TodoQueryPort todos;

    private CampaignOverviewService service;

    @BeforeEach
    void setUp() {
        service = new CampaignOverviewService(campaigns, sessions, clocks, looseThreads, arcs, beats, todos, clock);
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(true);
        // Every other port's unstubbed calls default to an empty list (Mockito's built-in
        // ReturnsEmptyValues) — each test below stubs only the port(s) it actually cares about.
    }

    @Test
    void unknownCampaignIsNotFound() {
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);
        assertThatThrownBy(() -> service.overview(worldId, campaignId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void nextSessionIsNearestStrictlyFutureDatedSession() {
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(
                session("Past", LocalDate.parse("2026-03-01")),
                session("Today, not next", LocalDate.parse("2026-03-03")),
                session("Nearest future", LocalDate.parse("2026-03-05")),
                session("Far off", LocalDate.parse("2026-04-01")),
                session("Undated", null)));

        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.nextSession()).isNotNull();
        assertThat(stats.nextSession().title()).isEqualTo("Nearest future");
    }

    @Test
    void nextSessionIsNullWhenNothingScheduled() {
        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.nextSession()).isNull();
    }

    @Test
    void openClocksExcludeFullOnesSortMostFilledFirstAndCarryNextUnfilledSegmentTitle() {
        ClockView nearlyThere = clockView("Nearly there", List.of(
                new ClockSegmentView(true, "Segment A", null),
                new ClockSegmentView(false, "Segment B", null)));
        ClockView justStarted = clockView("Just started", List.of(
                new ClockSegmentView(false, "Segment A", null),
                new ClockSegmentView(false, "Segment B", null)));
        ClockView complete = clockView("Complete", List.of(new ClockSegmentView(true, "Segment A", null)));
        when(clocks.findByCampaign(campaignId)).thenReturn(List.of(nearlyThere, justStarted, complete));

        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.openClocksNearFilling()).extracting("title")
                .containsExactly("Nearly there", "Just started");
        assertThat(stats.openClocksNearFilling().get(0).nextUnfilledSegmentTitle()).isEqualTo("Segment B");
    }

    @Test
    void openLooseThreadsExcludeResolvedAndSortNewestFirst() {
        when(looseThreads.findByCampaign(campaignId)).thenReturn(List.of(
                thread("Older open thread", "OPEN", Instant.parse("2026-01-01T00:00:00Z")),
                thread("Newer open thread", "OPEN", Instant.parse("2026-02-01T00:00:00Z")),
                thread("Resolved thread", "RESOLVED", Instant.parse("2026-03-01T00:00:00Z"))));

        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.openLooseThreads()).extracting("text")
                .containsExactly("Newer open thread", "Older open thread");
    }

    @Test
    void openBeatsOnlyIncludeActiveArcsAndExcludeDoneBeats() {
        UUID activeArcId = UUID.randomUUID();
        UUID plannedArcId = UUID.randomUUID();
        when(arcs.findByCampaign(campaignId)).thenReturn(List.of(
                arc(activeArcId, "The Rebellion", "ACTIVE"),
                arc(plannedArcId, "Someday", "PLANNED")));
        when(beats.findByArc(activeArcId)).thenReturn(List.of(
                beat("Open beat", false),
                beat("Done beat", true)));
        // beats.findByArc(plannedArcId) is deliberately never stubbed/called — a PLANNED arc is
        // filtered out before its beats are ever queried.

        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.openBeatsInActiveArcs()).hasSize(1);
        assertThat(stats.openBeatsInActiveArcs().get(0).beatTitle()).isEqualTo("Open beat");
        assertThat(stats.openBeatsInActiveArcs().get(0).arcTitle()).isEqualTo("The Rebellion");
    }

    @Test
    void nextSessionTodosAreTheNextSessionsUndoneTodosOnly() {
        UUID sessionId = UUID.randomUUID();
        when(sessions.findOrdered(campaignId)).thenReturn(List.of(
                new SessionView(sessionId, campaignId, "Next up", null, LocalDate.parse("2026-03-05"), null, null,
                        Instant.EPOCH, Instant.EPOCH)));
        when(todos.findBySession(sessionId)).thenReturn(List.of(
                todo(sessionId, "Print handout", false),
                todo(sessionId, "Already done", true)));

        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.nextSessionTodos()).extracting("text").containsExactly("Print handout");
    }

    @Test
    void nextSessionTodosIsEmptyWhenThereIsNoNextSession() {
        CampaignOverviewStats stats = service.overview(worldId, campaignId);

        assertThat(stats.nextSessionTodos()).isEmpty();
    }

    private SessionView session(String title, LocalDate date) {
        return new SessionView(UUID.randomUUID(), campaignId, title, null, date, null, null, Instant.EPOCH,
                Instant.EPOCH);
    }

    private ClockView clockView(String title, List<ClockSegmentView> segments) {
        return new ClockView(UUID.randomUUID(), campaignId, title, null, segments, 0, Instant.EPOCH, Instant.EPOCH);
    }

    private LooseThreadView thread(String text, String status, Instant createdAt) {
        return new LooseThreadView(UUID.randomUUID(), UUID.randomUUID(), campaignId, text, status, createdAt,
                createdAt);
    }

    private ArcView arc(UUID id, String title, String status) {
        return new ArcView(id, campaignId, title, null, status, 0, Instant.EPOCH, Instant.EPOCH);
    }

    private ArcBeatView beat(String title, boolean done) {
        return new ArcBeatView(UUID.randomUUID(), UUID.randomUUID(), title, null, done, List.of(), List.of(),
                List.of(), List.of(), List.of(), null, null, 0, Instant.EPOCH, Instant.EPOCH);
    }

    private TodoView todo(UUID sessionId, String text, boolean done) {
        return new TodoView(UUID.randomUUID(), campaignId, sessionId, text, done, Instant.EPOCH, Instant.EPOCH);
    }
}
