package com.campaignorganizer.relationship;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {

    List<Relationship> findByWorldIdOrderByCreatedAtAsc(UUID worldId);

    Optional<Relationship> findByIdAndWorldId(UUID id, UUID worldId);

    /** Edges where the article is either endpoint (its neighbourhood). */
    @Query("""
            SELECT r FROM Relationship r
            WHERE r.worldId = :worldId
              AND (r.fromArticleId = :articleId OR r.toArticleId = :articleId)
            ORDER BY r.createdAt ASC
            """)
    List<Relationship> findTouchingArticle(@Param("worldId") UUID worldId,
                                           @Param("articleId") UUID articleId);
}
