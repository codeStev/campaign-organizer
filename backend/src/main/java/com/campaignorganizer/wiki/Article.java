package com.campaignorganizer.wiki;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "articles")
public class Article {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "category_id")
    private UUID categoryId;

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

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Article() {
        // for JPA
    }

    public Article(UUID worldId, UUID categoryId, String title, String slug,
                   ArticleTemplate template, String body) {
        this.worldId = worldId;
        this.categoryId = categoryId;
        this.title = title;
        this.slug = slug;
        this.template = template;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (template == null) {
            template = ArticleTemplate.GENERIC;
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(UUID categoryId, String title, String slug,
                       ArticleTemplate template, String body) {
        this.categoryId = categoryId;
        this.title = title;
        this.slug = slug;
        this.template = template;
        this.body = body;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getCategoryId() {
        return categoryId;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
