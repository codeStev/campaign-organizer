package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.CreateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;

public interface CreateTimelineUseCase {

    TimelineView create(CreateTimelineCommand command);
}
