package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a map. */
public record MapView(
        UUID id,
        UUID worldId,
        String name,
        UUID mediaId,
        Instant createdAt,
        Instant updatedAt) {
}
