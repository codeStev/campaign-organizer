package com.campaignorganizer.worldbuilding.application.calendar.port.in;

import java.util.List;
import java.util.UUID;

public final class CalendarCommands {

    private CalendarCommands() {
    }

    public record MonthInput(String name, int days) {
    }

    public record CreateCalendarCommand(UUID worldId, String name, Integer daysPerWeek,
                                        List<MonthInput> months) {
    }

    public record UpdateCalendarCommand(UUID worldId, UUID calendarId, String name, Integer daysPerWeek,
                                        List<MonthInput> months) {
    }
}
