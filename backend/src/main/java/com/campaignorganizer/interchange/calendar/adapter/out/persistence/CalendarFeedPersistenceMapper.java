package com.campaignorganizer.interchange.calendar.adapter.out.persistence;

import com.campaignorganizer.interchange.calendar.domain.CalendarFeed;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CalendarFeedPersistenceMapper {

    CalendarFeedJpaEntity toEntity(CalendarFeed feed);

    default CalendarFeed toDomain(CalendarFeedJpaEntity e) {
        if (e == null) {
            return null;
        }
        return CalendarFeed.reconstitute(e.getCampaignId(), e.getToken(), e.getCreatedAt());
    }
}
