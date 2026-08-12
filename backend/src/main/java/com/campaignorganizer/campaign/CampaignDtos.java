package com.campaignorganizer.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CampaignDtos {

    private CampaignDtos() {
    }

    public record CampaignRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description,
            @Size(max = 20000) String notes) {
    }

    public record CampaignResponse(
            UUID id,
            UUID worldId,
            String name,
            String description,
            String notes,
            Instant createdAt,
            Instant updatedAt) {

        public static CampaignResponse from(Campaign c) {
            return new CampaignResponse(c.getId(), c.getWorldId(), c.getName(),
                    c.getDescription(), c.getNotes(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
