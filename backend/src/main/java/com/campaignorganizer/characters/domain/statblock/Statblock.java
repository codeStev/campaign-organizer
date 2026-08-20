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
    private UUID articleId;
    private UUID campaignId;
    private UUID templateId;
    private String name;
    private Map<String, Object> stats;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private Statblock(UUID id, UUID worldId, UUID articleId, UUID campaignId, UUID templateId, String name,
                      Map<String, Object> stats, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleId, campaignId, templateId, name, stats, notes);
    }

    public static Statblock create(UUID id, UUID worldId, UUID articleId, UUID campaignId, UUID templateId,
                                   String name, Map<String, Object> stats, String notes, Instant now) {
        return new Statblock(id, worldId, articleId, campaignId, templateId, name, stats, notes, now, now);
    }

    public static Statblock reconstitute(UUID id, UUID worldId, UUID articleId, UUID campaignId,
                                         UUID templateId, String name, Map<String, Object> stats, String notes,
                                         Instant createdAt, Instant updatedAt) {
        return new Statblock(id, worldId, articleId, campaignId, templateId, name, stats, notes, createdAt,
                updatedAt);
    }

    public void update(UUID articleId, UUID campaignId, UUID templateId, String name,
                       Map<String, Object> stats, String notes, Instant now) {
        apply(articleId, campaignId, templateId, name, stats, notes);
        this.updatedAt = now;
    }

    private void apply(UUID articleId, UUID campaignId, UUID templateId, String name,
                       Map<String, Object> stats, String notes) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Statblock name must not be blank");
        }
        this.articleId = articleId;
        this.campaignId = campaignId;
        this.templateId = templateId;
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

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getTemplateId() {
        return templateId;
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
