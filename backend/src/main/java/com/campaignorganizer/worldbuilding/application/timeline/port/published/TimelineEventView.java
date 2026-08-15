package com.campaignorganizer.worldbuilding.application.timeline.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a timeline event. */
public record TimelineEventView(
        UUID id,
        UUID timelineId,
        UUID articleId,
        String title,
        String description,
        int year,
        Integer month,
        Integer day,
        Instant createdAt,
        Instant updatedAt) {
}
