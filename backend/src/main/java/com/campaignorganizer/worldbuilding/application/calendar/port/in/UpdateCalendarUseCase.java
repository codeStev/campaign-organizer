package com.campaignorganizer.worldbuilding.application.calendar.port.in;

import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.UpdateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;

public interface UpdateCalendarUseCase {

    CalendarView update(UpdateCalendarCommand command);
}
