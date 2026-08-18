package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.in.TimelineCommands.UpdateTimelineCommand;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;

public interface UpdateTimelineUseCase {

    TimelineView update(UpdateTimelineCommand command);
}
