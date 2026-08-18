package com.campaignorganizer.worldbuilding.domain.wiki;

import java.time.Instant;
import java.util.UUID;

/** An immutable snapshot of an article's content at a point in time (ADR-0026). */
public final class ArticleRevision {

    private final UUID id;
    private final UUID articleId;
    private final String title;
    private final String slug;
    private final ArticleTemplate template;
    private final String body;
    private final Instant createdAt;

    private ArticleRevision(UUID id, UUID articleId, String title, String slug, ArticleTemplate template,
                            String body, Instant createdAt) {
        this.id = id;
        this.articleId = articleId;
        this.title = title;
        this.slug = slug;
        this.template = template;
        this.body = body;
        this.createdAt = createdAt;
    }

    /** Capture the current content of an article as a new revision. */
    public static ArticleRevision snapshot(UUID id, Article article, Instant now) {
        return new ArticleRevision(id, article.getId(), article.getTitle(), article.getSlug(),
                article.getTemplate(), article.getBody(), now);
    }

    public static ArticleRevision reconstitute(UUID id, UUID articleId, String title, String slug,
                                               ArticleTemplate template, String body, Instant createdAt) {
        return new ArticleRevision(id, articleId, title, slug, template, body, createdAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public ArticleTemplate getTemplate() {
        return template;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
