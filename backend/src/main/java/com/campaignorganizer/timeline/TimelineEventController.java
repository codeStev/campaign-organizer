package com.campaignorganizer.timeline;

import com.campaignorganizer.calendar.CalendarMonth;
import com.campaignorganizer.calendar.CalendarMonthRepository;
import com.campaignorganizer.timeline.TimelineEventDtos.TimelineEventRequest;
import com.campaignorganizer.timeline.TimelineEventDtos.TimelineEventResponse;
import com.campaignorganizer.wiki.ArticleRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/timelines/{timelineId}/events")
public class TimelineEventController {

    private final TimelineEventRepository events;
    private final TimelineRepository timelines;
    private final ArticleRepository articles;
    private final CalendarMonthRepository calendarMonths;

    public TimelineEventController(TimelineEventRepository events, TimelineRepository timelines,
                                   ArticleRepository articles, CalendarMonthRepository calendarMonths) {
        this.events = events;
        this.timelines = timelines;
        this.articles = articles;
        this.calendarMonths = calendarMonths;
    }

    @GetMapping
    public List<TimelineEventResponse> list(@PathVariable UUID worldId, @PathVariable UUID timelineId) {
        requireTimeline(worldId, timelineId);
        return events.findOrdered(timelineId).stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TimelineEventResponse> create(@PathVariable UUID worldId,
                                                        @PathVariable UUID timelineId,
                                                        @Valid @RequestBody TimelineEventRequest request) {
        Timeline timeline = requireTimeline(worldId, timelineId);
        validateArticle(worldId, request.articleId());
        validateAgainstCalendar(timeline, request.month(), request.day());
        TimelineEvent saved = events.save(new TimelineEvent(timelineId, request.articleId(),
                request.title(), request.description(), request.year(), request.month(), request.day()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/timelines/" + timelineId
                        + "/events/" + saved.getId()))
                .body(TimelineEventResponse.from(saved));
    }

    @PutMapping("/{eventId}")
    public TimelineEventResponse update(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                                        @PathVariable UUID eventId,
                                        @Valid @RequestBody TimelineEventRequest request) {
        Timeline timeline = requireTimeline(worldId, timelineId);
        validateArticle(worldId, request.articleId());
        validateAgainstCalendar(timeline, request.month(), request.day());
        TimelineEvent event = events.findByIdAndTimelineId(eventId, timelineId)
                .orElseThrow(this::eventNotFound);
        event.update(request.articleId(), request.title(), request.description(),
                request.year(), request.month(), request.day());
        return TimelineEventResponse.from(events.save(event));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                       @PathVariable UUID eventId) {
        requireTimeline(worldId, timelineId);
        TimelineEvent event = events.findByIdAndTimelineId(eventId, timelineId)
                .orElseThrow(this::eventNotFound);
        events.delete(event);
    }

    private Timeline requireTimeline(UUID worldId, UUID timelineId) {
        return timelines.findByIdAndWorldId(timelineId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeline not found"));
    }

    private void validateArticle(UUID worldId, UUID articleId) {
        if (articleId != null && !articles.existsByIdAndWorldId(articleId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
        }
    }

    /** When the timeline has a calendar, month/day must fit its month definitions (ADR-0020). */
    private void validateAgainstCalendar(Timeline timeline, Integer month, Integer day) {
        if (timeline.getCalendarId() == null || month == null) {
            return;
        }
        List<CalendarMonth> months = calendarMonths.findByCalendarIdOrderByPositionAsc(timeline.getCalendarId());
        if (months.isEmpty()) {
            return;
        }
        if (month < 1 || month > months.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Month is out of range for the timeline's calendar");
        }
        if (day != null) {
            int length = months.get(month - 1).getDays();
            if (day < 1 || day > length) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Day is out of range for that month");
            }
        }
    }

    private ResponseStatusException eventNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
    }
}
