package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Alternate names for an article (ADR-0116), feeding the same
 * {@code [[link]]} resolution index as title/slug. */
public interface ArticleAliasRepositoryPort {

    List<String> findByArticle(UUID worldId, UUID articleId);

    /** Every alias in the world, grouped by article id - for building the
     * shared {@code [[link]]} resolution index (see {@code ArticleRefIndex}). */
    Map<UUID, List<String>> findAllByWorld(UUID worldId);

    /** Whole-set replace, mirroring the tags feature's PUT semantics. */
    void replaceAll(UUID worldId, UUID articleId, List<String> aliases);
}
