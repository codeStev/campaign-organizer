package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineEventWebDtos.TimelineEventRequest;
import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineEventWebDtos.TimelineEventResponse;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.CreateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.UpdateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelineEventWebMapper {

    TimelineEventResponse toResponse(TimelineEventView view);

    default CreateTimelineEventCommand toCreateCommand(UUID worldId, UUID timelineId,
                                                       TimelineEventRequest request) {
        return new CreateTimelineEventCommand(worldId, timelineId, request.articleId(), request.title(),
                request.description(), request.year(), request.month(), request.day());
    }

    default UpdateTimelineEventCommand toUpdateCommand(UUID worldId, UUID timelineId, UUID eventId,
                                                       TimelineEventRequest request) {
        return new UpdateTimelineEventCommand(worldId, timelineId, eventId, request.articleId(),
                request.title(), request.description(), request.year(), request.month(), request.day());
    }
}
