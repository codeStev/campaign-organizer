package com.campaignorganizer.characters.adapter.statblock.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class StatblockWebDtos {

    private StatblockWebDtos() {
    }

    public record StatblockRequest(
            @NotBlank @Size(max = 200) String name,
            UUID articleId,
            UUID campaignId,
            UUID templateId,
            Map<String, Object> stats,
            @Size(max = 20000) String notes) {
    }

    public record StatblockResponse(
            UUID id,
            UUID worldId,
            UUID articleId,
            UUID campaignId,
            UUID templateId,
            String name,
            Map<String, Object> stats,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
    }
}
