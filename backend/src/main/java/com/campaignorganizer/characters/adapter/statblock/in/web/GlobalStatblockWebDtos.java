package com.campaignorganizer.characters.adapter.statblock.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class GlobalStatblockWebDtos {

    private GlobalStatblockWebDtos() {
    }

    public record GlobalStatblockRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull UUID systemId,
            UUID globalTemplateId,
            Map<String, Object> stats,
            @Size(max = 20000) String notes) {
    }

    public record GlobalStatblockResponse(
            UUID id,
            UUID systemId,
            UUID globalTemplateId,
            String name,
            Map<String, Object> stats,
            String notes,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ImportGlobalStatblockRequest(
            @NotNull UUID worldId,
            @NotNull UUID campaignId,
            @Size(max = 200) String name) {
    }
}
