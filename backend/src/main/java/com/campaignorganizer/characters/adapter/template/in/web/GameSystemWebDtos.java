package com.campaignorganizer.characters.adapter.template.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class GameSystemWebDtos {

    private GameSystemWebDtos() {
    }

    public record GameSystemRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String tagline,
            @Size(max = 20) String color,
            String notes) {
    }

    public record GameSystemResponse(
            UUID id,
            String name,
            String tagline,
            String color,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
    }
}
