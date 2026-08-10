package com.campaignorganizer.wiki;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class CategoryDtos {

    private CategoryDtos() {
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

        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getWorldId(), c.getParentId(),
                    c.getName(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
