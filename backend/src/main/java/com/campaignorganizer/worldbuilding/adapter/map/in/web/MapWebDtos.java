package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class MapWebDtos {

    private MapWebDtos() {
    }

    public record MapRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull UUID mediaId) {
    }

    public record MapResponse(
            UUID id,
            UUID worldId,
            String name,
            UUID mediaId,
            String imageUrl,
            Instant createdAt,
            Instant updatedAt) {
    }
}
