package com.campaignorganizer.worldbuilding.application.relationship.port.out;

import com.campaignorganizer.worldbuilding.domain.relationship.Relationship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelationshipRepositoryPort {

    List<Relationship> findByWorld(UUID worldId);

    Optional<Relationship> findByIdAndWorld(UUID relationshipId, UUID worldId);

    List<Relationship> findTouchingArticle(UUID worldId, UUID articleId);

    Relationship save(Relationship relationship);

    void delete(Relationship relationship);
}
