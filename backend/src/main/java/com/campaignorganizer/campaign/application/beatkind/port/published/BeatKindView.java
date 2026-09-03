package com.campaignorganizer.campaign.application.beatkind.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a beat kind. */
public record BeatKindView(
        UUID id,
        UUID worldId,
        String name,
        String color,
        Instant createdAt,
        Instant updatedAt) {
}
