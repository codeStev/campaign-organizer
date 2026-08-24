package com.campaignorganizer.worldbuilding.application.calendar.port.published;

/**
 * Published port: persists a calendar exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface CalendarImportPort {

    CalendarView importCalendar(CalendarView view);
}
