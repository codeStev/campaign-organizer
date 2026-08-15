package com.campaignorganizer.timeline;

import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.timeline.TimelineDtos.TimelineRequest;
import com.campaignorganizer.timeline.TimelineDtos.TimelineResponse;
import com.campaignorganizer.world.WorldRepository;
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
@RequestMapping("/api/worlds/{worldId}/timelines")
public class TimelineController {

    private final TimelineRepository timelines;
    private final WorldRepository worlds;
    private final CalendarQueryPort calendars;

    public TimelineController(TimelineRepository timelines, WorldRepository worlds,
                              CalendarQueryPort calendars) {
        this.timelines = timelines;
        this.worlds = worlds;
        this.calendars = calendars;
    }

    @GetMapping
    public List<TimelineResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return timelines.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(TimelineResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TimelineResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody TimelineRequest request) {
        requireWorld(worldId);
        validateCalendar(worldId, request.calendarId());
        Timeline saved = timelines.save(
                new Timeline(worldId, request.name(), request.description(), request.calendarId()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/timelines/" + saved.getId()))
                .body(TimelineResponse.from(saved));
    }

    @PutMapping("/{timelineId}")
    public TimelineResponse update(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                                   @Valid @RequestBody TimelineRequest request) {
        Timeline timeline = findOrThrow(worldId, timelineId);
        validateCalendar(worldId, request.calendarId());
        timeline.update(request.name(), request.description(), request.calendarId());
        return TimelineResponse.from(timelines.save(timeline));
    }

    @DeleteMapping("/{timelineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID timelineId) {
        timelines.delete(findOrThrow(worldId, timelineId));
    }

    private Timeline findOrThrow(UUID worldId, UUID timelineId) {
        return timelines.findByIdAndWorldId(timelineId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Timeline not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateCalendar(UUID worldId, UUID calendarId) {
        if (calendarId != null && !calendars.existsInWorld(calendarId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Calendar not found in this world");
        }
    }
}
