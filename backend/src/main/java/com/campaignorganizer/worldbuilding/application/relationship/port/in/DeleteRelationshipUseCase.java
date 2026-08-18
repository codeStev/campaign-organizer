package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import java.util.UUID;

public interface DeleteRelationshipUseCase {

    void delete(UUID worldId, UUID relationshipId);
}
