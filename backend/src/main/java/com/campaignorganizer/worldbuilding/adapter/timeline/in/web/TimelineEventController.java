package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineEventWebDtos.TimelineEventRequest;
import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineEventWebDtos.TimelineEventResponse;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.CreateTimelineEventUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.DeleteTimelineEventUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.ListTimelineEventsUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.UpdateTimelineEventUseCase;
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

@RestController
@RequestMapping("/api/worlds/{worldId}/timelines/{timelineId}/events")
public class TimelineEventController {

    private final CreateTimelineEventUseCase createUseCase;
    private final UpdateTimelineEventUseCase updateUseCase;
    private final DeleteTimelineEventUseCase deleteUseCase;
    private final ListTimelineEventsUseCase listUseCase;
    private final TimelineEventWebMapper mapper;

    public TimelineEventController(CreateTimelineEventUseCase createUseCase,
                                   UpdateTimelineEventUseCase updateUseCase,
                                   DeleteTimelineEventUseCase deleteUseCase,
                                   ListTimelineEventsUseCase listUseCase, TimelineEventWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TimelineEventResponse> list(@PathVariable UUID worldId, @PathVariable UUID timelineId) {
        return listUseCase.list(worldId, timelineId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TimelineEventResponse> create(@PathVariable UUID worldId,
                                                        @PathVariable UUID timelineId,
                                                        @Valid @RequestBody TimelineEventRequest request) {
        TimelineEventResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, timelineId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/timelines/" + timelineId
                        + "/events/" + response.id()))
                .body(response);
    }

    @PutMapping("/{eventId}")
    public TimelineEventResponse update(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                                        @PathVariable UUID eventId,
                                        @Valid @RequestBody TimelineEventRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, timelineId, eventId, request)));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                       @PathVariable UUID eventId) {
        deleteUseCase.delete(worldId, timelineId, eventId);
    }
}
