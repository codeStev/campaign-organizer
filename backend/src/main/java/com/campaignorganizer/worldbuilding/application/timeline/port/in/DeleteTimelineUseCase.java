package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import java.util.UUID;

public interface DeleteTimelineUseCase {

    void delete(UUID worldId, UUID timelineId);
}
