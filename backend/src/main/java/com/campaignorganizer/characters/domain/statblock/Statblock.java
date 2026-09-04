package com.campaignorganizer.characters.domain.statblock;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A set of game stats, optionally scoped to a campaign and linked to an article (aggregate root). */
public final class Statblock {

    private final UUID id;
    private final UUID worldId;
    private UUID categoryId;
    private UUID articleId;
    private UUID campaignId;
    private UUID worldTemplateId;
    private UUID globalTemplateId;
    private String name;
    private Map<String, Object> stats;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private Statblock(UUID id, UUID worldId, UUID categoryId, UUID articleId, UUID campaignId,
                      UUID worldTemplateId, UUID globalTemplateId, String name, Map<String, Object> stats,
                      String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleId, campaignId, worldTemplateId, globalTemplateId, name, stats, notes);
    }

    public static Statblock create(UUID id, UUID worldId, UUID categoryId, UUID articleId, UUID campaignId,
                                   UUID worldTemplateId, UUID globalTemplateId, String name,
                                   Map<String, Object> stats, String notes, Instant now) {
        return new Statblock(id, worldId, categoryId, articleId, campaignId, worldTemplateId, globalTemplateId,
                name, stats, notes, now, now);
    }

    public static Statblock reconstitute(UUID id, UUID worldId, UUID categoryId, UUID articleId,
                                         UUID campaignId, UUID worldTemplateId, UUID globalTemplateId,
                                         String name, Map<String, Object> stats, String notes,
                                         Instant createdAt, Instant updatedAt) {
        return new Statblock(id, worldId, categoryId, articleId, campaignId, worldTemplateId, globalTemplateId,
                name, stats, notes, createdAt, updatedAt);
    }

    public void update(UUID categoryId, UUID articleId, UUID campaignId, UUID worldTemplateId,
                       UUID globalTemplateId, String name, Map<String, Object> stats, String notes,
                       Instant now) {
        apply(articleId, campaignId, worldTemplateId, globalTemplateId, name, stats, notes);
        this.categoryId = categoryId;
        this.updatedAt = now;
    }

    private void apply(UUID articleId, UUID campaignId, UUID worldTemplateId, UUID globalTemplateId,
                       String name, Map<String, Object> stats, String notes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Statblock name must not be blank");
        }
        if (worldTemplateId != null && globalTemplateId != null) {
            throw new ValidationException(
                    "Statblock cannot have both worldTemplateId and globalTemplateId set");
        }
        this.articleId = articleId;
        this.campaignId = campaignId;
        this.worldTemplateId = worldTemplateId;
        this.globalTemplateId = globalTemplateId;
        this.name = name;
        this.stats = stats == null ? new HashMap<>() : stats;
        this.notes = notes;
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

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getWorldTemplateId() {
        return worldTemplateId;
    }

    public UUID getGlobalTemplateId() {
        return globalTemplateId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getStats() {
        return stats;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
