package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineWebDtos.TimelineRequest;
import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineWebDtos.TimelineResponse;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.CreateTimelineUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.DeleteTimelineUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.ListTimelinesUseCase;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.UpdateTimelineUseCase;
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
@RequestMapping("/api/worlds/{worldId}/timelines")
public class TimelineController {

    private final CreateTimelineUseCase createUseCase;
    private final UpdateTimelineUseCase updateUseCase;
    private final DeleteTimelineUseCase deleteUseCase;
    private final ListTimelinesUseCase listUseCase;
    private final TimelineWebMapper mapper;

    public TimelineController(CreateTimelineUseCase createUseCase, UpdateTimelineUseCase updateUseCase,
                             DeleteTimelineUseCase deleteUseCase, ListTimelinesUseCase listUseCase,
                             TimelineWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TimelineResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TimelineResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody TimelineRequest request) {
        TimelineResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/timelines/" + response.id()))
                .body(response);
    }

    @PutMapping("/{timelineId}")
    public TimelineResponse update(@PathVariable UUID worldId, @PathVariable UUID timelineId,
                                   @Valid @RequestBody TimelineRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(worldId, timelineId, request)));
    }

    @DeleteMapping("/{timelineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID timelineId) {
        deleteUseCase.delete(worldId, timelineId);
    }
}
