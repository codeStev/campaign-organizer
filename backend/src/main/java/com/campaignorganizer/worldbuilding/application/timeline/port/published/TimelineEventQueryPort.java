package com.campaignorganizer.worldbuilding.application.timeline.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read timeline events from other contexts (usage, export). */
public interface TimelineEventQueryPort {

    List<TimelineEventView> findByArticle(UUID articleId);

    List<TimelineEventView> findByTimeline(UUID timelineId);
}
