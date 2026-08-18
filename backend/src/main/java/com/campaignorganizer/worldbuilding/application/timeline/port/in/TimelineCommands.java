package com.campaignorganizer.worldbuilding.application.timeline.port.in;

import java.util.UUID;

public final class TimelineCommands {

    private TimelineCommands() {
    }

    public record CreateTimelineCommand(UUID worldId, String name, String description, UUID calendarId) {
    }

    public record UpdateTimelineCommand(UUID worldId, UUID timelineId, String name, String description,
                                        UUID calendarId) {
    }
}
