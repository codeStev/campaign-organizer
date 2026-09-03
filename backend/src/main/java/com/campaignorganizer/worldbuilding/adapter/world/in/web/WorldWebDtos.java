package com.campaignorganizer.worldbuilding.adapter.world.in.web;

import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Request/response payloads for the worlds resource (mirrors docs/api/openapi.yaml). */
public final class WorldWebDtos {

    private WorldWebDtos() {
    }

    public record WorldRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description,
            Boolean scratch) {
    }

    public record WorldResponse(
            UUID id,
            String name,
            String description,
            Map<String, LayerStyle> layerStyles,
            boolean scratch,
            Instant createdAt,
            Instant updatedAt) {
    }
}
