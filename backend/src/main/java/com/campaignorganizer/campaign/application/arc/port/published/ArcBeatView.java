package com.campaignorganizer.campaign.application.arc.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a story beat. */
public record ArcBeatView(
        UUID id,
        UUID arcId,
        String title,
        String body,
        boolean done,
        List<UUID> articleIds,
        List<UUID> statblockIds,
        List<UUID> tableIds,
        List<UUID> deckIds,
        UUID sessionId,
        int position,
        Instant createdAt,
        Instant updatedAt) {
}
