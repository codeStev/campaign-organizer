package com.campaignorganizer.timeline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    /** Chronological order; year-only events sort ahead of dated ones in the same year. */
    @Query("""
            SELECT e FROM TimelineEvent e
            WHERE e.timelineId = :timelineId
            ORDER BY e.year ASC, e.month ASC NULLS FIRST, e.day ASC NULLS FIRST
            """)
    List<TimelineEvent> findOrdered(@Param("timelineId") UUID timelineId);

    Optional<TimelineEvent> findByIdAndTimelineId(UUID id, UUID timelineId);
}
