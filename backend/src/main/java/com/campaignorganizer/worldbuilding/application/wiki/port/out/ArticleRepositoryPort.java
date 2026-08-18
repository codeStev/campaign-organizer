package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepositoryPort {

    List<Article> findByWorld(UUID worldId);

    List<Article> findByWorldAndCategory(UUID worldId, UUID categoryId);

    List<Article> search(UUID worldId, String query);

    Optional<Article> findByIdAndWorld(UUID articleId, UUID worldId);

    Optional<Article> findById(UUID articleId);

    boolean existsInWorld(UUID articleId, UUID worldId);

    boolean existsSlugInWorld(UUID worldId, String slug);

    /** Lightweight id/title projection keyed by lowercase slug and title, for auto-link resolution. */
    List<ArticleRef> findRefsByWorld(UUID worldId);

    Article save(Article article);

    void delete(Article article);

    /** Id/slug/title projection used to build the auto-link index (ADR-0014). */
    record ArticleRef(UUID id, String slug, String title) {
    }
}
