package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import java.util.List;
import java.util.UUID;

public interface ListTimelinesUseCase {

    List<TimelineView> list(UUID worldId);
}
