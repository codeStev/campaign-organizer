package com.campaignorganizer.world;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Request/response payloads for the worlds resource (mirrors docs/api/openapi.yaml). */
public final class WorldDtos {

    private WorldDtos() {
    }

    public record WorldRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 5000) String description) {
    }

    public record WorldResponse(
            UUID id,
            String name,
            String description,
            Map<String, LayerStyle> layerStyles,
            Instant createdAt,
            Instant updatedAt) {

        public static WorldResponse from(World world) {
            return new WorldResponse(
                    world.getId(),
                    world.getName(),
                    world.getDescription(),
                    world.getLayerStyles(),
                    world.getCreatedAt(),
                    world.getUpdatedAt());
        }
    }
}
