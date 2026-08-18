package com.campaignorganizer.worldbuilding.adapter.calendar.in.web;

import com.campaignorganizer.worldbuilding.adapter.calendar.in.web.CalendarWebDtos.CalendarRequest;
import com.campaignorganizer.worldbuilding.adapter.calendar.in.web.CalendarWebDtos.CalendarResponse;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CreateCalendarUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.DeleteCalendarUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.ListCalendarsUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.UpdateCalendarUseCase;
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

/** Thin web adapter for calendars. */
@RestController
@RequestMapping("/api/worlds/{worldId}/calendars")
public class CalendarController {

    private final CreateCalendarUseCase createUseCase;
    private final UpdateCalendarUseCase updateUseCase;
    private final DeleteCalendarUseCase deleteUseCase;
    private final ListCalendarsUseCase listUseCase;
    private final CalendarWebMapper mapper;

    public CalendarController(CreateCalendarUseCase createUseCase, UpdateCalendarUseCase updateUseCase,
                              DeleteCalendarUseCase deleteUseCase, ListCalendarsUseCase listUseCase,
                              CalendarWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CalendarResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<CalendarResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CalendarRequest request) {
        CalendarResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/calendars/" + response.id()))
                .body(response);
    }

    @PutMapping("/{calendarId}")
    public CalendarResponse update(@PathVariable UUID worldId, @PathVariable UUID calendarId,
                                   @Valid @RequestBody CalendarRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, calendarId, request)));
    }

    @DeleteMapping("/{calendarId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID calendarId) {
        deleteUseCase.delete(worldId, calendarId);
    }
}
