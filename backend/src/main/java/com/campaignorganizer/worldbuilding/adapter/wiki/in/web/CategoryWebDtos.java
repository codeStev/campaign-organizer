package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CategoryWebDtos {

    private CategoryWebDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 200) String name,
            UUID parentId) {
    }

    public record CategoryResponse(
            UUID id,
            UUID worldId,
            UUID parentId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
