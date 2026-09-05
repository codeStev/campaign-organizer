package com.campaignorganizer.interchange.calendar.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarFeedJpaRepository extends JpaRepository<CalendarFeedJpaEntity, UUID> {

    Optional<CalendarFeedJpaEntity> findByToken(UUID token);
}
