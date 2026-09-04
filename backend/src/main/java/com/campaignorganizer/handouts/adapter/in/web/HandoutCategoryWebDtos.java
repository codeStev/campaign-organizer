package com.campaignorganizer.handouts.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class HandoutCategoryWebDtos {

    private HandoutCategoryWebDtos() {
    }

    public record HandoutCategoryRequest(
            @NotBlank @Size(max = 200) String name,
            UUID parentId) {
    }

    public record HandoutCategoryResponse(
            UUID id,
            UUID worldId,
            UUID parentId,
            String name,
            Instant createdAt,
            Instant updatedAt) {
    }
}
