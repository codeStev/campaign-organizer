package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import com.campaignorganizer.worldbuilding.domain.timeline.TimelineEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelineEventPersistenceMapper {

    TimelineEventJpaEntity toEntity(TimelineEvent event);

    default TimelineEvent toDomain(TimelineEventJpaEntity e) {
        if (e == null) {
            return null;
        }
        return TimelineEvent.reconstitute(e.getId(), e.getTimelineId(), e.getArticleId(), e.getTitle(),
                e.getDescription(), e.getYear(), e.getMonth(), e.getDay(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
