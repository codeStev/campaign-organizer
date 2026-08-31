package com.campaignorganizer.campaign.application.clock.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a clock. */
public record ClockView(
        UUID id,
        UUID campaignId,
        String title,
        String description,
        List<ClockSegmentView> segments,
        int position,
        Instant createdAt,
        Instant updatedAt) {
}
