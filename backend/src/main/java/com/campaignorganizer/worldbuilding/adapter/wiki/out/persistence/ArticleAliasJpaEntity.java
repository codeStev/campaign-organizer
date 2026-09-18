package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/** Maps the {@code article_aliases} table (ADR-0116) — a flat, no-frills
 * value row (no surrogate id/created_at, unlike {@code entity_tags}): an
 * alias has no independent identity beyond "this article has this name". */
@Entity
@Table(name = "article_aliases")
@IdClass(ArticleAliasId.class)
public class ArticleAliasJpaEntity {

    @Id
    @Column(name = "article_id", updatable = false)
    private UUID articleId;

    @Id
    @Column(nullable = false, length = 200, updatable = false)
    private String alias;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    protected ArticleAliasJpaEntity() {
        // for JPA
    }

    public ArticleAliasJpaEntity(UUID articleId, UUID worldId, String alias) {
        this.articleId = articleId;
        this.worldId = worldId;
        this.alias = alias;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getAlias() {
        return alias;
    }
}
