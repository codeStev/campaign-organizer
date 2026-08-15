package com.campaignorganizer.worldbuilding.application.timeline.service;

import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import com.campaignorganizer.worldbuilding.domain.timeline.Timeline;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelineViewMapper {

    TimelineView toView(Timeline timeline);
}
