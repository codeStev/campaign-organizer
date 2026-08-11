package com.campaignorganizer.map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class MapDtos {

    private MapDtos() {
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

        public static MapResponse from(WorldMap map) {
            String imageUrl = map.getMediaId() == null
                    ? null
                    : "/api/media/" + map.getMediaId() + "/content";
            return new MapResponse(map.getId(), map.getWorldId(), map.getName(),
                    map.getMediaId(), imageUrl, map.getCreatedAt(), map.getUpdatedAt());
        }
    }
}
