package com.campaignorganizer.campaign.adapter.arc.in.web;

import com.campaignorganizer.campaign.domain.arc.ArcStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ArcWebDtos {

    private ArcWebDtos() {
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
    }
}
