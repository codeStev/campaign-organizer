package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineEventCommands.UpdateTimelineEventCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;

public interface UpdateTimelineEventUseCase {

    TimelineEventView update(UpdateTimelineEventCommand command);
}
