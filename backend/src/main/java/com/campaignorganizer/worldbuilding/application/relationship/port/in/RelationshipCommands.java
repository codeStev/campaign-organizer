package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import java.util.UUID;

public final class RelationshipCommands {

    private RelationshipCommands() {
    }

    public record CreateRelationshipCommand(UUID worldId, UUID fromArticleId, UUID toArticleId,
                                            String label, boolean directed) {
    }

    public record UpdateRelationshipCommand(UUID worldId, UUID relationshipId, UUID fromArticleId,
                                            UUID toArticleId, String label, boolean directed) {
    }
}
