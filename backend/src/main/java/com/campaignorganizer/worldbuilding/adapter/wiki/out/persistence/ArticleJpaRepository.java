package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleJpaRepository extends JpaRepository<ArticleJpaEntity, UUID> {

    List<ArticleJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<ArticleJpaEntity> findByWorldIdAndCategoryIdOrderByCreatedAtDesc(UUID worldId, UUID categoryId);

    Optional<ArticleJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByWorldIdAndSlug(UUID worldId, String slug);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);

    /** Lightweight id/slug/title projection used for auto-link resolution (ADR-0014). */
    @Query("SELECT a.id AS id, a.slug AS slug, a.title AS title FROM ArticleJpaEntity a "
            + "WHERE a.worldId = :worldId")
    List<ArticleRefProjection> findRefsByWorldId(@Param("worldId") UUID worldId);

    interface ArticleRefProjection {
        UUID getId();

        String getSlug();

        String getTitle();
    }

    /**
     * Fuzzy article search within a world (ADR-0022). Matches partial words via
     * ILIKE and tolerates typos via trigram similarity, so "water" and
     * "watredeep" both find "Waterdeep". Title substring matches rank first,
     * then trigram closeness, then body matches.
     */
    @Query(value = """
            SELECT * FROM articles a
            WHERE a.world_id = :worldId
              AND (
                a.title ILIKE '%' || :q || '%'
                OR a.body ILIKE '%' || :q || '%'
                OR similarity(a.title, :q) > 0.15
              )
            ORDER BY
              CASE WHEN a.title ILIKE '%' || :q || '%' THEN 0 ELSE 1 END,
              similarity(a.title, :q) DESC,
              a.updated_at DESC
            """, nativeQuery = true)
    List<ArticleJpaEntity> search(@Param("worldId") UUID worldId, @Param("q") String q);
}
