package com.campaignorganizer.worldbuilding.adapter.calendar.in.web;

import com.campaignorganizer.worldbuilding.adapter.calendar.in.web.CalendarWebDtos.CalendarRequest;
import com.campaignorganizer.worldbuilding.adapter.calendar.in.web.CalendarWebDtos.CalendarResponse;
import com.campaignorganizer.worldbuilding.adapter.calendar.in.web.CalendarWebDtos.MonthDto;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.CreateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.MonthInput;
import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.UpdateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarMonthView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps calendar web DTOs ↔ commands/view (MapStruct). */
@Mapper(componentModel = "spring")
public interface CalendarWebMapper {

    CalendarResponse toResponse(CalendarView view);

    MonthDto toMonthDto(CalendarMonthView view);

    MonthInput toMonthInput(MonthDto dto);

    List<MonthInput> toMonthInputs(List<MonthDto> months);

    default CreateCalendarCommand toCreateCommand(UUID worldId, CalendarRequest request) {
        return new CreateCalendarCommand(worldId, request.name(), request.daysPerWeek(),
                toMonthInputs(request.months()));
    }

    default UpdateCalendarCommand toUpdateCommand(UUID worldId, UUID calendarId, CalendarRequest request) {
        return new UpdateCalendarCommand(worldId, calendarId, request.name(), request.daysPerWeek(),
                toMonthInputs(request.months()));
    }
}
