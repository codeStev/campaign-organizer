package com.campaignorganizer.worldbuilding.domain.wiki;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A wiki article within a world (aggregate root). */
public final class Article {

    private final UUID id;
    private final UUID worldId;
    private UUID categoryId;
    private String title;
    private String slug;
    private ArticleTemplate template;
    private String body;
    private final Instant createdAt;
    private Instant updatedAt;

    private Article(UUID id, UUID worldId, UUID categoryId, String title, String slug,
                    ArticleTemplate template, String body, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(categoryId, title, slug, template, body);
    }

    public static Article create(UUID id, UUID worldId, UUID categoryId, String title, String slug,
                                 ArticleTemplate template, String body, Instant now) {
        return new Article(id, worldId, categoryId, title, slug, template, body, now, now);
    }

    public static Article reconstitute(UUID id, UUID worldId, UUID categoryId, String title, String slug,
                                       ArticleTemplate template, String body, Instant createdAt,
                                       Instant updatedAt) {
        return new Article(id, worldId, categoryId, title, slug, template, body, createdAt, updatedAt);
    }

    public void update(UUID categoryId, String title, String slug, ArticleTemplate template, String body,
                       Instant now) {
        apply(categoryId, title, slug, template, body);
        this.updatedAt = now;
    }

    private void apply(UUID categoryId, String title, String slug, ArticleTemplate template, String body) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Article title must not be blank");
        }
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Article slug must not be blank");
        }
        this.categoryId = categoryId;
        this.title = title;
        this.slug = slug;
        this.template = template == null ? ArticleTemplate.GENERIC : template;
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
