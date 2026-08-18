package com.campaignorganizer.worldbuilding.application.relationship.port.out;

import java.util.UUID;

/** Checks an article exists in a world (endpoint validation). */
public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
