package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimelineEventJpaRepository extends JpaRepository<TimelineEventJpaEntity, UUID> {

    @Query("""
            SELECT e FROM TimelineEventJpaEntity e
            WHERE e.timelineId = :timelineId
            ORDER BY e.year ASC, e.month ASC NULLS FIRST, e.day ASC NULLS FIRST
            """)
    List<TimelineEventJpaEntity> findOrdered(@Param("timelineId") UUID timelineId);

    Optional<TimelineEventJpaEntity> findByIdAndTimelineId(UUID id, UUID timelineId);

    List<TimelineEventJpaEntity> findByArticleId(UUID articleId);
}
