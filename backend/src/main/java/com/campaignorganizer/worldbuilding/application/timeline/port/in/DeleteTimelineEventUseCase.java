package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import java.util.UUID;

public interface DeleteTimelineEventUseCase {

    void delete(UUID worldId, UUID timelineId, UUID eventId);
}
