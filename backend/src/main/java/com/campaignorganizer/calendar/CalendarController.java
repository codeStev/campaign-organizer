package com.campaignorganizer.calendar;

import com.campaignorganizer.calendar.CalendarDtos.CalendarMonthInput;
import com.campaignorganizer.calendar.CalendarDtos.CalendarRequest;
import com.campaignorganizer.calendar.CalendarDtos.CalendarResponse;
import com.campaignorganizer.world.WorldRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/worlds/{worldId}/calendars")
public class CalendarController {

    private final CalendarRepository calendars;
    private final CalendarMonthRepository months;
    private final WorldRepository worlds;

    public CalendarController(CalendarRepository calendars, CalendarMonthRepository months,
                              WorldRepository worlds) {
        this.calendars = calendars;
        this.months = months;
        this.worlds = worlds;
    }

    @GetMapping
    public List<CalendarResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return calendars.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(c -> CalendarResponse.from(c, monthsOf(c.getId())))
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CalendarResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CalendarRequest request) {
        requireWorld(worldId);
        Calendar saved = calendars.save(new Calendar(worldId, request.name(), request.daysPerWeek()));
        saveMonths(saved.getId(), request.months());
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/calendars/" + saved.getId()))
                .body(CalendarResponse.from(saved, monthsOf(saved.getId())));
    }

    @PutMapping("/{calendarId}")
    @Transactional
    public CalendarResponse update(@PathVariable UUID worldId, @PathVariable UUID calendarId,
                                   @Valid @RequestBody CalendarRequest request) {
        Calendar calendar = findOrThrow(worldId, calendarId);
        calendar.update(request.name(), request.daysPerWeek());
        calendars.save(calendar);
        months.deleteByCalendarId(calendarId);
        saveMonths(calendarId, request.months());
        return CalendarResponse.from(calendar, monthsOf(calendarId));
    }

    @DeleteMapping("/{calendarId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID calendarId) {
        calendars.delete(findOrThrow(worldId, calendarId));
    }

    private void saveMonths(UUID calendarId, List<CalendarMonthInput> inputs) {
        for (int i = 0; i < inputs.size(); i++) {
            CalendarMonthInput input = inputs.get(i);
            months.save(new CalendarMonth(calendarId, i, input.name(), input.days()));
        }
    }

    private List<CalendarMonth> monthsOf(UUID calendarId) {
        return months.findByCalendarIdOrderByPositionAsc(calendarId);
    }

    private Calendar findOrThrow(UUID worldId, UUID calendarId) {
        return calendars.findByIdAndWorldId(calendarId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendar not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }
}
