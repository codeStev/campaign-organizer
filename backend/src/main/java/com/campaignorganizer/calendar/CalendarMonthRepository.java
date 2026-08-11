package com.campaignorganizer.calendar;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarMonthRepository extends JpaRepository<CalendarMonth, UUID> {

    List<CalendarMonth> findByCalendarIdOrderByPositionAsc(UUID calendarId);

    void deleteByCalendarId(UUID calendarId);
}
