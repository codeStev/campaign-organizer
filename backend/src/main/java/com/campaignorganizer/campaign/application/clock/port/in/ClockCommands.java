package com.campaignorganizer.campaign.application.clock.port.in;

import java.util.List;
import java.util.UUID;

public final class ClockCommands {

    private ClockCommands() {
    }

    public record SegmentInput(boolean filled, String title, String description) {
    }

    /** {@code position} may be null; the service defaults it (mirrors ADR-0032 for arcs). */
    public record CreateClockCommand(UUID worldId, UUID campaignId, String title, String description,
                                     List<SegmentInput> segments, Integer position) {
    }

    public record UpdateClockCommand(UUID worldId, UUID campaignId, UUID clockId, String title,
                                     String description, List<SegmentInput> segments, Integer position) {
    }
}
