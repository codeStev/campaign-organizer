package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.CreateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;

public interface CreateTimelineEventUseCase {

    TimelineEventView create(CreateTimelineEventCommand command);
}
