package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class MapPinWebDtos {

    private MapPinWebDtos() {
    }

    public record MapPinRequest(
            UUID articleId,
            @Size(max = 200) String label,
            @Size(max = 100) String layer,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double x,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double y) {
    }

    public record MapPinResponse(
            UUID id,
            UUID mapId,
            UUID articleId,
            String label,
            String layer,
            double x,
            double y,
            Instant createdAt,
            Instant updatedAt) {
    }
}
