package com.campaignorganizer.worldbuilding.application.calendar.port.in;

import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import java.util.List;
import java.util.UUID;

public interface ListCalendarsUseCase {

    List<CalendarView> list(UUID worldId);
}
