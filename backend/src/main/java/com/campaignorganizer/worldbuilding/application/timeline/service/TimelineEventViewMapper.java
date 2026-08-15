package com.campaignorganizer.worldbuilding.application.timeline.service;

import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import com.campaignorganizer.worldbuilding.domain.timeline.TimelineEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelineEventViewMapper {

    TimelineEventView toView(TimelineEvent event);
}
