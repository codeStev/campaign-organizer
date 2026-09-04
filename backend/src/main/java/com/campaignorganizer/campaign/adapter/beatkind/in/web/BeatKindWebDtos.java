package com.campaignorganizer.campaign.adapter.beatkind.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class BeatKindWebDtos {

    private BeatKindWebDtos() {
    }

    public record BeatKindRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 20) String color) {
    }

    public record BeatKindResponse(
            UUID id,
            UUID worldId,
            String name,
            String color,
            Instant createdAt,
            Instant updatedAt) {
    }
}
