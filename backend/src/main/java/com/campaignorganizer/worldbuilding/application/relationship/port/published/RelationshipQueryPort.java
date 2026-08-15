package com.campaignorganizer.worldbuilding.application.relationship.port.published;

import java.util.List;
import java.util.UUID;

/** Published query port: read relationships from other contexts (usage, export). */
public interface RelationshipQueryPort {

    List<RelationshipView> findByWorld(UUID worldId);

    List<RelationshipView> findTouchingArticle(UUID worldId, UUID articleId);
}
