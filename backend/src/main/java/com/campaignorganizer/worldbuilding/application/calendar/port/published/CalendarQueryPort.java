package com.campaignorganizer.worldbuilding.application.calendar.port.published;

import java.util.List;
import java.util.UUID;

/**
 * Published port for other contexts (timeline date validation, export). Exposes
 * calendar existence and month definitions without leaking the domain/persistence.
 */
public interface CalendarQueryPort {

    boolean existsInWorld(UUID calendarId, UUID worldId);

    /** The ordered months of a calendar (empty if it has none / does not exist). */
    List<CalendarMonthView> monthsOf(UUID calendarId);

    List<CalendarView> findByWorld(UUID worldId);
}
