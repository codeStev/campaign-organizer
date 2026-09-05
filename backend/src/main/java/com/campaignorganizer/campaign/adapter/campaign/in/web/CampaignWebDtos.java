package com.campaignorganizer.campaign.adapter.campaign.in.web;

import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CampaignWebDtos {

    private CampaignWebDtos() {
    }

    public record CampaignRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description,
            @Size(max = 20000) String notes,
            CampaignStatus status,
            UUID systemId,
            @Size(max = 20) String color) {
    }

    public record CampaignResponse(
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
}
