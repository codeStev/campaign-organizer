package com.campaignorganizer.worldbuilding.application.timeline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarMonthView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.CreateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.TimelineEventRepositoryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineLookupPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test (incl. the calendar date rule) with mocked ports. */
@ExtendWith(MockitoExtension.class)
class TimelineEventServiceTest {

    @Mock
    private TimelineEventRepositoryPort events;
    @Mock
    private TimelineLookupPort timelines;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private CalendarQueryPort calendars;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final TimelineEventViewMapper viewMapper = new TimelineEventViewMapperImpl();

    private TimelineEventService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID timelineId = UUID.randomUUID();
    private final UUID calendarId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new TimelineEventService(events, timelines, articles, calendars, viewMapper, ids, clock);
    }

    private TimelineView timeline(UUID calId) {
        return new TimelineView(timelineId, worldId, "T", null, calId,
                Instant.now(), Instant.now());
    }

    @Test
    void createWithoutCalendarSucceeds() {
        when(timelines.findById(timelineId)).thenReturn(Optional.of(timeline(null)));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TimelineEventView view = service.create(new CreateTimelineEventCommand(
                worldId, timelineId, null, "Founding", null, 100, null, null));

        assertThat(view.title()).isEqualTo("Founding");
    }

    @Test
    void createRejectsMissingTimeline() {
        when(timelines.findById(timelineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateTimelineEventCommand(
                worldId, timelineId, null, "x", null, 1, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignArticle() {
        when(timelines.findById(timelineId)).thenReturn(Optional.of(timeline(null)));
        when(articles.existsInWorld(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateTimelineEventCommand(
                worldId, timelineId, UUID.randomUUID(), "x", null, 1, null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsMonthOutOfCalendarRange() {
        when(timelines.findById(timelineId)).thenReturn(Optional.of(timeline(calendarId)));
        when(calendars.monthsOf(calendarId)).thenReturn(List.of(
                new CalendarMonthView("Jan", 30), new CalendarMonthView("Feb", 28)));

        assertThatThrownBy(() -> service.create(new CreateTimelineEventCommand(
                worldId, timelineId, null, "x", null, 1, 5, 1)))
                .isInstanceOf(ValidationException.class);
    }
}
