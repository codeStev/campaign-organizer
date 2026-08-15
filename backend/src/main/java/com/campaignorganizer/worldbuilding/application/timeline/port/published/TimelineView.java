package com.campaignorganizer.worldbuilding.application.timeline.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a timeline. */
public record TimelineView(
        UUID id,
        UUID worldId,
        String name,
        String description,
        UUID calendarId,
        Instant createdAt,
        Instant updatedAt) {
}
