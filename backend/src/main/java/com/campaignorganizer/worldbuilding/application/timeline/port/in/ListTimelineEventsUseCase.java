package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import java.util.List;
import java.util.UUID;

public interface ListTimelineEventsUseCase {

    List<TimelineEventView> list(UUID worldId, UUID timelineId);
}
