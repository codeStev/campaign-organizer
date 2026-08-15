package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import java.util.UUID;

public final class TimelineEventCommands {

    private TimelineEventCommands() {
    }

    public record CreateTimelineEventCommand(UUID worldId, UUID timelineId, UUID articleId, String title,
                                             String description, int year, Integer month, Integer day) {
    }

    public record UpdateTimelineEventCommand(UUID worldId, UUID timelineId, UUID eventId, UUID articleId,
                                             String title, String description, int year, Integer month,
                                             Integer day) {
    }
}
