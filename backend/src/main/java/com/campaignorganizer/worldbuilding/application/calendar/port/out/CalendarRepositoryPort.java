package com.campaignorganizer.worldbuilding.application.calendar.port.out;

import com.campaignorganizer.worldbuilding.domain.calendar.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarRepositoryPort {

    List<Calendar> findByWorld(UUID worldId);

    Optional<Calendar> findByIdAndWorld(UUID calendarId, UUID worldId);

    Optional<Calendar> findById(UUID calendarId);

    boolean existsInWorld(UUID calendarId, UUID worldId);

    Calendar save(Calendar calendar);

    void delete(Calendar calendar);
}
