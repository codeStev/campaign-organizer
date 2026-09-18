package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key for {@link ArticleAliasJpaEntity} — {@code (article_id, alias)}. */
public final class ArticleAliasId implements Serializable {

    private UUID articleId;
    private String alias;

    public ArticleAliasId() {
        // for JPA
    }

    public ArticleAliasId(UUID articleId, String alias) {
        this.articleId = articleId;
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArticleAliasId other)) {
            return false;
        }
        return Objects.equals(articleId, other.articleId) && Objects.equals(alias, other.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(articleId, alias);
    }
}
