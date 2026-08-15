package com.campaignorganizer.worldbuilding.application.timeline.port.out;

import com.campaignorganizer.worldbuilding.domain.timeline.TimelineEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimelineEventRepositoryPort {

    List<TimelineEvent> findByTimelineOrdered(UUID timelineId);

    List<TimelineEvent> findByArticle(UUID articleId);

    Optional<TimelineEvent> findByIdAndTimeline(UUID eventId, UUID timelineId);

    TimelineEvent save(TimelineEvent event);

    void delete(TimelineEvent event);
}
