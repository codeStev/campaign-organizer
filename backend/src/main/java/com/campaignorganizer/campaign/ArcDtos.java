package com.campaignorganizer.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ArcDtos {

    private ArcDtos() {
    }

    public record ArcRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 5000) String description,
            ArcStatus status,
            Integer position) {
    }

    public record ArcResponse(
            UUID id,
            UUID campaignId,
            String title,
            String description,
            ArcStatus status,
            int position,
            Instant createdAt,
            Instant updatedAt) {

        public static ArcResponse from(Arc a) {
            return new ArcResponse(a.getId(), a.getCampaignId(), a.getTitle(), a.getDescription(),
                    a.getStatus(), a.getPosition(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
