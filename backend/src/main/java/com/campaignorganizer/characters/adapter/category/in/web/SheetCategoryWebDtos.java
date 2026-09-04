package com.campaignorganizer.characters.adapter.category.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class SheetCategoryWebDtos {

    private SheetCategoryWebDtos() {
    }

    public record SheetCategoryRequest(
            @NotBlank @Size(max = 200) String name,
            UUID parentId) {
    }

    public record SheetCategoryResponse(
            UUID id,
            UUID worldId,
            UUID parentId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
