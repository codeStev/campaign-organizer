package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import com.campaignorganizer.worldbuilding.domain.timeline.Timeline;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelinePersistenceMapper {

    TimelineJpaEntity toEntity(Timeline timeline);

    default Timeline toDomain(TimelineJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Timeline.reconstitute(entity.getId(), entity.getWorldId(), entity.getName(),
                entity.getDescription(), entity.getCalendarId(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
