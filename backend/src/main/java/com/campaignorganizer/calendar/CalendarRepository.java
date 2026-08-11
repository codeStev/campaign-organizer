package com.campaignorganizer.calendar;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {

    List<Calendar> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<Calendar> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
