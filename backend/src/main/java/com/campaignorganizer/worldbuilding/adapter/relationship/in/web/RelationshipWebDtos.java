package com.campaignorganizer.worldbuilding.adapter.relationship.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/** Web request/response models for relationships. */
public final class RelationshipWebDtos {

    private RelationshipWebDtos() {
    }

    public record RelationshipRequest(
            @NotNull UUID fromArticleId,
            @NotNull UUID toArticleId,
            @Size(max = 100) String label,
            Boolean directed) {

        public boolean directedOrDefault() {
            return directed == null || directed;
        }
    }

    public record RelationshipResponse(
            UUID id,
            UUID worldId,
            UUID fromArticleId,
            UUID toArticleId,
            String label,
            boolean directed,
            Instant createdAt,
            Instant updatedAt) {
    }
}
