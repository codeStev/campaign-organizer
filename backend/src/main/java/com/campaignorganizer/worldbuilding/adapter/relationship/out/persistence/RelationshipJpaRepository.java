package com.campaignorganizer.worldbuilding.adapter.relationship.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelationshipJpaRepository extends JpaRepository<RelationshipJpaEntity, UUID> {

    List<RelationshipJpaEntity> findByWorldIdOrderByCreatedAtAsc(UUID worldId);

    Optional<RelationshipJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    @Query("""
            SELECT r FROM RelationshipJpaEntity r
            WHERE r.worldId = :worldId
              AND (r.fromArticleId = :articleId OR r.toArticleId = :articleId)
            ORDER BY r.createdAt ASC
            """)
    List<RelationshipJpaEntity> findTouchingArticle(@Param("worldId") UUID worldId,
                                                    @Param("articleId") UUID articleId);
}
