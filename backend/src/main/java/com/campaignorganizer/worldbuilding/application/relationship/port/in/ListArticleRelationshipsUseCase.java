package com.campaignorganizer.worldbuilding.application.relationship.port.in;

import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import java.util.List;
import java.util.UUID;

/** Relationships touching a single article (its neighbourhood). */
public interface ListArticleRelationshipsUseCase {

    List<RelationshipView> listForArticle(UUID worldId, UUID articleId);
}
