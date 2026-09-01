package com.campaignorganizer.campaign.application.player.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a player. */
public record PlayerView(
        UUID id,
        UUID worldId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
