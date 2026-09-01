package com.campaignorganizer.campaign.application.campaign.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for one campaign roster membership. */
public record CampaignPlayerView(UUID id, UUID campaignId, UUID playerId, boolean guest,
                                 Instant createdAt) {
}
