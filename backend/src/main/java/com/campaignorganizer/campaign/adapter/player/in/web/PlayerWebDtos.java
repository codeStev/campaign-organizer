package com.campaignorganizer.campaign.adapter.player.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class PlayerWebDtos {

    private PlayerWebDtos() {
    }

    public record PlayerRequest(
            @NotBlank @Size(max = 200) String name) {
    }

    public record PlayerResponse(
            UUID id,
            UUID worldId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
