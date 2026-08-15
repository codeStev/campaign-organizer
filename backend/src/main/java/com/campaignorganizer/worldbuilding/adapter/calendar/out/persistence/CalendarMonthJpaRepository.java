package com.campaignorganizer.worldbuilding.adapter.calendar.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarMonthJpaRepository extends JpaRepository<CalendarMonthJpaEntity, UUID> {

    List<CalendarMonthJpaEntity> findByCalendarIdOrderByPositionAsc(UUID calendarId);

    void deleteByCalendarId(UUID calendarId);
}
