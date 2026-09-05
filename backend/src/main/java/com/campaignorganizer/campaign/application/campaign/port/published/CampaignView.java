package com.campaignorganizer.campaign.application.campaign.port.published;

import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import java.time.Instant;
import java.util.UUID;

/** Published read model for a campaign. */
public record CampaignView(
        UUID id,
        UUID worldId,
        String name,
        String description,
        String notes,
        CampaignStatus status,
        UUID systemId,
        String color,
        Instant createdAt,
        Instant updatedAt) {
}
