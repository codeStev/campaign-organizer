package com.campaignorganizer.worldbuilding.adapter.calendar.out.persistence;

import com.campaignorganizer.worldbuilding.application.calendar.port.out.CalendarRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.calendar.Calendar;
import com.campaignorganizer.worldbuilding.domain.calendar.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed calendar aggregate persistence across the calendars + calendar_months tables. */
@Component
public class CalendarPersistenceAdapter implements CalendarRepositoryPort {

    private final CalendarJpaRepository calendars;
    private final CalendarMonthJpaRepository months;
    private final CalendarPersistenceMapper mapper;

    public CalendarPersistenceAdapter(CalendarJpaRepository calendars, CalendarMonthJpaRepository months,
                                      CalendarPersistenceMapper mapper) {
        this.calendars = calendars;
        this.months = months;
        this.mapper = mapper;
    }

    @Override
    public List<Calendar> findByWorld(UUID worldId) {
        return calendars.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Calendar> findByIdAndWorld(UUID calendarId, UUID worldId) {
        return calendars.findByIdAndWorldId(calendarId, worldId).map(this::toDomain);
    }

    @Override
    public Optional<Calendar> findById(UUID calendarId) {
        return calendars.findById(calendarId).map(this::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID calendarId, UUID worldId) {
        return calendars.existsByIdAndWorldId(calendarId, worldId);
    }

    @Override
    public Calendar save(Calendar calendar) {
        calendars.save(mapper.toEntity(calendar));
        // Months are replaced wholesale (position = list index).
        months.deleteByCalendarId(calendar.getId());
        List<Month> ms = calendar.getMonths();
        for (int i = 0; i < ms.size(); i++) {
            Month m = ms.get(i);
            CalendarMonthJpaEntity row = new CalendarMonthJpaEntity();
            row.setId(UUID.randomUUID());
            row.setCalendarId(calendar.getId());
            row.setPosition(i);
            row.setName(m.name());
            row.setDays(m.days());
            months.save(row);
        }
        return calendar;
    }

    @Override
    public void delete(Calendar calendar) {
        months.deleteByCalendarId(calendar.getId());
        calendars.deleteById(calendar.getId());
    }

    private Calendar toDomain(CalendarJpaEntity entity) {
        List<Month> monthList = months.findByCalendarIdOrderByPositionAsc(entity.getId()).stream()
                .map(mapper::toMonth)
                .toList();
        return Calendar.reconstitute(entity.getId(), entity.getWorldId(), entity.getName(),
                entity.getDaysPerWeek(), monthList, entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
