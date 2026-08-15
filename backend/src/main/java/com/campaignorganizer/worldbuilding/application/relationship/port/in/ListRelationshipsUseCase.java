package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import java.util.List;
import java.util.UUID;

public interface ListRelationshipsUseCase {

    List<RelationshipView> list(UUID worldId);
}
