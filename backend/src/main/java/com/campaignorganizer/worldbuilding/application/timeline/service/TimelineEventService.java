package com.campaignorganizer.worldbuilding.application.timeline.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarMonthView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.CreateTimelineEventUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.DeleteTimelineEventUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.ListTimelineEventsUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.CreateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.UpdateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.UpdateTimelineEventUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.TimelineEventRepositoryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventImportPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineLookupPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import com.campaignorganizer.worldbuilding.domain.timeline.TimelineEvent;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Timeline-event use cases; implements the published query port for consumers. */
@Service
public class TimelineEventService implements CreateTimelineEventUseCase, UpdateTimelineEventUseCase,
        DeleteTimelineEventUseCase, ListTimelineEventsUseCase, TimelineEventQueryPort,
        TimelineEventImportPort {

    private final TimelineEventRepositoryPort events;
    private final TimelineLookupPort timelines;
    private final ArticleExistsPort articles;
    private final CalendarQueryPort calendars;
    private final TimelineEventViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public TimelineEventService(TimelineEventRepositoryPort events, TimelineLookupPort timelines,
                                ArticleExistsPort articles, CalendarQueryPort calendars,
                                TimelineEventViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.events = events;
        this.timelines = timelines;
        this.articles = articles;
        this.calendars = calendars;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TimelineEventView create(CreateTimelineEventCommand command) {
        TimelineView timeline = requireTimeline(command.worldId(), command.timelineId());
        requireArticle(command.worldId(), command.articleId());
        validateAgainstCalendar(timeline, command.month(), command.day());
        TimelineEvent created = TimelineEvent.create(ids.newId(), command.timelineId(),
                command.articleId(), command.title(), command.description(), command.year(),
                command.month(), command.day(), clock.instant());
        return viewMapper.toView(events.save(created));
    }

    @Override
    @Transactional
    public TimelineEventView update(UpdateTimelineEventCommand command) {
        TimelineView timeline = requireTimeline(command.worldId(), command.timelineId());
        TimelineEvent event = events.findByIdAndTimeline(command.eventId(), command.timelineId())
                .orElseThrow(() -> new NotFoundException("Event not found"));
        requireArticle(command.worldId(), command.articleId());
        validateAgainstCalendar(timeline, command.month(), command.day());
        event.update(command.articleId(), command.title(), command.description(), command.year(),
                command.month(), command.day(), clock.instant());
        return viewMapper.toView(events.save(event));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID timelineId, UUID eventId) {
        requireTimeline(worldId, timelineId);
        TimelineEvent event = events.findByIdAndTimeline(eventId, timelineId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        events.delete(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEventView> list(UUID worldId, UUID timelineId) {
        requireTimeline(worldId, timelineId);
        return events.findByTimelineOrdered(timelineId).stream().map(viewMapper::toView).toList();
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public TimelineEventView importTimelineEvent(TimelineEventView view) {
        TimelineEvent event = TimelineEvent.reconstitute(view.id(), view.timelineId(), view.articleId(),
                view.title(), view.description(), view.year(), view.month(), view.day(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(events.save(event));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEventView> findByArticle(UUID articleId) {
        return events.findByArticle(articleId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEventView> findByTimeline(UUID timelineId) {
        return events.findByTimelineOrdered(timelineId).stream().map(viewMapper::toView).toList();
    }

    private TimelineView requireTimeline(UUID worldId, UUID timelineId) {
        return timelines.findById(timelineId)
                .filter(t -> t.worldId().equals(worldId))
                .orElseThrow(() -> new NotFoundException("Timeline not found"));
    }

    private void requireArticle(UUID worldId, UUID articleId) {
        if (articleId != null && !articles.existsInWorld(articleId, worldId)) {
            throw new ValidationException("Article not found in this world");
        }
    }

    /** When the timeline has a calendar, month/day must fit its month definitions (ADR-0020). */
    private void validateAgainstCalendar(TimelineView timeline, Integer month, Integer day) {
        if (timeline.calendarId() == null || month == null) {
            return;
        }
        List<CalendarMonthView> months = calendars.monthsOf(timeline.calendarId());
        if (months.isEmpty()) {
            return;
        }
        if (month < 1 || month > months.size()) {
            throw new ValidationException("Month is out of range for the timeline's calendar");
        }
        if (day != null) {
            int length = months.get(month - 1).days();
            if (day < 1 || day > length) {
                throw new ValidationException("Day is out of range for that month");
            }
        }
    }
}
