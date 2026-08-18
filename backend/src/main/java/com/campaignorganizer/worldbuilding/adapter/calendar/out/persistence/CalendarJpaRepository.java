package com.campaignorganizer.worldbuilding.adapter.calendar.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarJpaRepository extends JpaRepository<CalendarJpaEntity, UUID> {

    List<CalendarJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<CalendarJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
