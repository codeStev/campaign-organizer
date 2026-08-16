package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a map pin. */
public record MapPinView(
        UUID id,
        UUID mapId,
        UUID articleId,
        String label,
        String layer,
        double x,
        double y,
        Instant createdAt,
        Instant updatedAt) {
}
