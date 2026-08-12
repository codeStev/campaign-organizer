package com.campaignorganizer.wiki;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** An immutable snapshot of an article's content at a point in time (ADR-0026). */
@Entity
@Table(name = "article_revisions")
public class ArticleRevision {

    @Id
    private UUID id;

    @Column(name = "article_id", nullable = false, updatable = false)
    private UUID articleId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 220)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArticleTemplate template;

    @Column(columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ArticleRevision() {
        // for JPA
    }

    private ArticleRevision(UUID articleId, String title, String slug, ArticleTemplate template, String body) {
        this.articleId = articleId;
        this.title = title;
        this.slug = slug;
        this.template = template;
        this.body = body;
    }

    /** Capture the current content of an article as a revision. */
    public static ArticleRevision of(Article article) {
        return new ArticleRevision(article.getId(), article.getTitle(), article.getSlug(),
                article.getTemplate(), article.getBody());
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
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
