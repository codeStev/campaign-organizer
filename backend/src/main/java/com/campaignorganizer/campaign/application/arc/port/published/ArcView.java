package com.campaignorganizer.campaign.application.arc.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a story arc. */
public record ArcView(
        UUID id,
        UUID campaignId,
        String title,
        String description,
        String status,
        int position,
        Instant createdAt,
        Instant updatedAt) {
}
