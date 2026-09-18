package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleAliasJpaRepository extends JpaRepository<ArticleAliasJpaEntity, ArticleAliasId> {

    List<ArticleAliasJpaEntity> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    List<ArticleAliasJpaEntity> findByWorldId(UUID worldId);

    /**
     * Bulk delete, not a derived {@code deleteBy...} method — see
     * {@code EntityTagJpaRepository.deleteByWorldIdAndEntityTypeAndEntityId}'s
     * javadoc for why: a derived delete only queues per-entity removals, and
     * Hibernate always flushes pending inserts before pending deletes within
     * one transaction, so a delete-then-reinsert replace-set would violate
     * this table's primary key for any alias kept across an edit.
     */
    @Modifying
    @Query("DELETE FROM ArticleAliasJpaEntity a WHERE a.worldId = :worldId AND a.articleId = :articleId")
    void deleteByWorldIdAndArticleId(@Param("worldId") UUID worldId, @Param("articleId") UUID articleId);
}
