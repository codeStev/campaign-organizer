package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read articles from other contexts and aggregates (usage, packet, export, links). */
public interface ArticleQueryPort {

    List<ArticleView> findByWorld(UUID worldId);

    Optional<ArticleView> findByIdInWorld(UUID articleId, UUID worldId);

    Optional<ArticleView> findById(UUID articleId);

    boolean existsInWorld(UUID articleId, UUID worldId);
}
