package com.campaignorganizer.worldbuilding.application.calendar.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a calendar (with its ordered months). */
public record CalendarView(
        UUID id,
        UUID worldId,
        String name,
        Integer daysPerWeek,
        List<CalendarMonthView> months,
        Instant createdAt,
        Instant updatedAt) {
}
