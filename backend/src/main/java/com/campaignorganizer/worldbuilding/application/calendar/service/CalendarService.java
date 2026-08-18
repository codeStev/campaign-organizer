package com.campaignorganizer.worldbuilding.application.calendar.service;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.CreateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.MonthInput;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.UpdateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CreateCalendarUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.DeleteCalendarUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.ListCalendarsUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.UpdateCalendarUseCase;
import com.campaignorganizer.worldbuilding.application.calendar.port.out.CalendarRepositoryPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarMonthView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import com.campaignorganizer.worldbuilding.domain.calendar.Calendar;
import com.campaignorganizer.worldbuilding.domain.calendar.Month;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calendar use cases; also implements the published query port for consumers. */
@Service
public class CalendarService implements CreateCalendarUseCase, UpdateCalendarUseCase,
        DeleteCalendarUseCase, ListCalendarsUseCase, CalendarQueryPort {

    private final CalendarRepositoryPort calendars;
    private final WorldExistsPort worlds;
    private final CalendarViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public CalendarService(CalendarRepositoryPort calendars, WorldExistsPort worlds,
                           CalendarViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.calendars = calendars;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CalendarView create(CreateCalendarCommand command) {
        requireWorld(command.worldId());
        Calendar created = Calendar.create(ids.newId(), command.worldId(), command.name(),
                command.daysPerWeek(), toMonths(command.months()), clock.instant());
        return viewMapper.toView(calendars.save(created));
    }

    @Override
    @Transactional
    public CalendarView update(UpdateCalendarCommand command) {
        Calendar calendar = require(command.worldId(), command.calendarId());
        calendar.update(command.name(), command.daysPerWeek(), toMonths(command.months()), clock.instant());
        return viewMapper.toView(calendars.save(calendar));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID calendarId) {
        calendars.delete(require(worldId, calendarId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarView> list(UUID worldId) {
        requireWorld(worldId);
        return calendars.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID calendarId, UUID worldId) {
        return calendars.existsInWorld(calendarId, worldId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarMonthView> monthsOf(UUID calendarId) {
        return calendars.findById(calendarId)
                .map(c -> c.getMonths().stream().map(viewMapper::toMonthView).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarView> findByWorld(UUID worldId) {
        return calendars.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    private List<Month> toMonths(List<MonthInput> inputs) {
        return inputs == null ? List.of()
                : inputs.stream().map(m -> new Month(m.name(), m.days())).toList();
    }

    private Calendar require(UUID worldId, UUID calendarId) {
        return calendars.findByIdAndWorld(calendarId, worldId)
                .orElseThrow(() -> new NotFoundException("Calendar not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}
