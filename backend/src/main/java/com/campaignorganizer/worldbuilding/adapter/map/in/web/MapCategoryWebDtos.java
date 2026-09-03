package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class MapCategoryWebDtos {

    private MapCategoryWebDtos() {
    }

    public record MapCategoryRequest(
            @NotBlank @Size(max = 200) String name,
            UUID parentId) {
    }

    public record MapCategoryResponse(
            UUID id,
            UUID worldId,
            UUID parentId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
