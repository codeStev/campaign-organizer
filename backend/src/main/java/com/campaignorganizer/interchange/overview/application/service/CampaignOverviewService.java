package com.campaignorganizer.interchange.overview.application.service;

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
import com.campaignorganizer.campaign.domain.arc.ArcStatus;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignLooseThreadSummary;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignNextSessionSummary;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignOverviewStats;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignTodoSummary;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.ClockPrepHint;
import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.OpenBeatSummary;
import com.campaignorganizer.interchange.overview.application.port.in.GetCampaignOverviewUseCase;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-campaign dashboard data (issue #67): next session, clocks close to
 * filling with a prep hint, open loose threads, open beats in active-status
 * arcs, and the next session's open todos. Pure read composition over
 * existing published ports — no new aggregate, no persisted state, same
 * shape as {@link WorldOverviewService} at world scope.
 */
@Service
public class CampaignOverviewService implements GetCampaignOverviewUseCase {

    private static final int OPEN_CLOCKS_LIMIT = 5;
    private static final int OPEN_LOOSE_THREADS_LIMIT = 8;
    private static final int OPEN_BEATS_LIMIT = 10;

    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ClockQueryPort clocks;
    private final LooseThreadQueryPort looseThreads;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final TodoQueryPort todos;
    private final Clock clock;

    public CampaignOverviewService(CampaignQueryPort campaigns, SessionQueryPort sessions, ClockQueryPort clocks,
                                   LooseThreadQueryPort looseThreads, ArcQueryPort arcs, ArcBeatQueryPort beats,
                                   TodoQueryPort todos, Clock clock) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.clocks = clocks;
        this.looseThreads = looseThreads;
        this.arcs = arcs;
        this.beats = beats;
        this.todos = todos;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignOverviewStats overview(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
        LocalDate today = LocalDate.now(clock);

        CampaignNextSessionSummary nextSession = nextSession(campaignId, today);
        List<ClockPrepHint> openClocksNearFilling = openClocksNearFilling(campaignId);
        List<CampaignLooseThreadSummary> openLooseThreads = openLooseThreads(campaignId);
        List<OpenBeatSummary> openBeatsInActiveArcs = openBeatsInActiveArcs(campaignId);
        List<CampaignTodoSummary> nextSessionTodos = nextSession == null
                ? List.of()
                : todos.findBySession(nextSession.sessionId()).stream()
                        .filter(t -> !t.done())
                        .map(t -> new CampaignTodoSummary(t.id(), t.text()))
                        .toList();

        return new CampaignOverviewStats(nextSession, openClocksNearFilling, openLooseThreads,
                openBeatsInActiveArcs, nextSessionTodos);
    }

    private CampaignNextSessionSummary nextSession(UUID campaignId, LocalDate today) {
        SessionView next = null;
        for (SessionView s : sessions.findOrdered(campaignId)) {
            if (s.date() == null || !s.date().isAfter(today)) {
                continue;
            }
            if (next == null || s.date().isBefore(next.date())) {
                next = s;
            }
        }
        return next == null ? null
                : new CampaignNextSessionSummary(next.id(), next.title(), next.date(), next.sessionNumber());
    }

    private List<ClockPrepHint> openClocksNearFilling(UUID campaignId) {
        return clocks.findByCampaign(campaignId).stream()
                .map(this::toClockPrepHint)
                .filter(hint -> hint.filledSegments() < hint.totalSegments())
                .sorted(Comparator.<ClockPrepHint>comparingDouble(
                        h -> (double) h.filledSegments() / h.totalSegments()).reversed())
                .limit(OPEN_CLOCKS_LIMIT)
                .toList();
    }

    private ClockPrepHint toClockPrepHint(ClockView clockView) {
        int filled = (int) clockView.segments().stream().filter(ClockSegmentView::filled).count();
        int total = clockView.segments().size();
        String nextUnfilledTitle = clockView.segments().stream()
                .filter(seg -> !seg.filled())
                .map(ClockSegmentView::title)
                .findFirst()
                .orElse(null);
        return new ClockPrepHint(clockView.id(), clockView.title(), filled, total, nextUnfilledTitle);
    }

    private List<CampaignLooseThreadSummary> openLooseThreads(UUID campaignId) {
        return looseThreads.findByCampaign(campaignId).stream()
                .filter(t -> "OPEN".equals(t.status()))
                .sorted(Comparator.comparing(LooseThreadView::createdAt).reversed())
                .limit(OPEN_LOOSE_THREADS_LIMIT)
                .map(t -> new CampaignLooseThreadSummary(t.id(), t.text()))
                .toList();
    }

    private List<OpenBeatSummary> openBeatsInActiveArcs(UUID campaignId) {
        return arcs.findByCampaign(campaignId).stream()
                .filter(a -> ArcStatus.ACTIVE.name().equals(a.status()))
                .flatMap(a -> beats.findByArc(a.id()).stream()
                        .filter(b -> !b.done())
                        .map(b -> toOpenBeatSummary(b, a)))
                .limit(OPEN_BEATS_LIMIT)
                .toList();
    }

    private OpenBeatSummary toOpenBeatSummary(ArcBeatView beat, ArcView arc) {
        return new OpenBeatSummary(beat.id(), arc.id(), arc.title(), beat.title());
    }
}
