package com.campaignorganizer.campaign.application.loosethread.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a loose thread. */
public record LooseThreadView(
        UUID id,
        UUID sessionId,
        UUID campaignId,
        String text,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
