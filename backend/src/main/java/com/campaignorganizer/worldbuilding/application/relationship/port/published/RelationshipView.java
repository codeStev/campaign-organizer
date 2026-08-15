package com.campaignorganizer.worldbuilding.application.relationship.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a relationship (used within the context and by consumers). */
public record RelationshipView(
        UUID id,
        UUID worldId,
        UUID fromArticleId,
        UUID toArticleId,
        String label,
        boolean directed,
        Instant createdAt,
        Instant updatedAt) {
}
