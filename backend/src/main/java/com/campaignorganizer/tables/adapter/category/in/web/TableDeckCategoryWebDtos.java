package com.campaignorganizer.tables.adapter.category.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class TableDeckCategoryWebDtos {

    private TableDeckCategoryWebDtos() {
    }

    public record TableDeckCategoryRequest(
            @NotBlank @Size(max = 200) String name,
            UUID parentId) {
    }

    public record TableDeckCategoryResponse(
            UUID id,
            UUID worldId,
            UUID parentId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
