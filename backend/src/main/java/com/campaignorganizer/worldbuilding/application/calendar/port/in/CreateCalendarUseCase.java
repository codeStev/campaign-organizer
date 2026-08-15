package com.campaignorganizer.worldbuilding.application.calendar.port.in;

import com.campaignorganizer.worldbuilding.application.calendar.port.in.CalendarCommands.CreateCalendarCommand;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;

public interface CreateCalendarUseCase {

    CalendarView create(CreateCalendarCommand command);
}
