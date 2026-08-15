package com.campaignorganizer.worldbuilding.adapter.timeline.in.web;

import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineWebDtos.TimelineRequest;
import com.campaignorganizer.worldbuilding.adapter.timeline.in.web.TimelineWebDtos.TimelineResponse;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.CreateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.UpdateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimelineWebMapper {

    TimelineResponse toResponse(TimelineView view);

    default CreateTimelineCommand toCreateCommand(UUID worldId, TimelineRequest request) {
        return new CreateTimelineCommand(worldId, request.name(), request.description(),
                request.calendarId());
    }

    default UpdateTimelineCommand toUpdateCommand(UUID worldId, UUID timelineId, TimelineRequest request) {
        return new UpdateTimelineCommand(worldId, timelineId, request.name(), request.description(),
                request.calendarId());
    }
}
