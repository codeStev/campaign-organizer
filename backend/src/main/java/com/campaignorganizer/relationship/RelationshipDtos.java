package com.campaignorganizer.relationship;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class RelationshipDtos {

    private RelationshipDtos() {
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

        public static RelationshipResponse from(Relationship r) {
            return new RelationshipResponse(r.getId(), r.getWorldId(), r.getFromArticleId(),
                    r.getToArticleId(), r.getLabel(), r.isDirected(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
