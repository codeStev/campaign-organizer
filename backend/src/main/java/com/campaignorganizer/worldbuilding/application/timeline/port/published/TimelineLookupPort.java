package com.campaignorganizer.worldbuilding.application.timeline.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read timelines from other contexts (usage, export). */
public interface TimelineLookupPort {

    Optional<TimelineView> findById(UUID timelineId);

    List<TimelineView> findByWorld(UUID worldId);
}
