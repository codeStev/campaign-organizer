package com.campaignorganizer.wiki;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    List<Article> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<Article> findByWorldIdAndCategoryIdOrderByCreatedAtDesc(UUID worldId, UUID categoryId);

    Optional<Article> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByWorldIdAndSlug(UUID worldId, String slug);

    /** Lightweight id/slug/title projection used for auto-link resolution (ADR-0014). */
    @Query("SELECT a.id AS id, a.slug AS slug, a.title AS title FROM Article a WHERE a.worldId = :worldId")
    List<ArticleRef> findRefsByWorldId(@Param("worldId") UUID worldId);

    interface ArticleRef {
        UUID getId();

        String getSlug();

        String getTitle();
    }

    /**
     * Ranked Postgres full-text search within a world (ADR-0017). Title matches
     * outrank body matches via the A/B weighting on the generated tsvector.
     */
    @Query(value = """
            SELECT * FROM articles
            WHERE world_id = :worldId
              AND search_vector @@ plainto_tsquery('english', :q)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :q)) DESC
            """, nativeQuery = true)
    List<Article> search(@Param("worldId") UUID worldId, @Param("q") String q);
}
