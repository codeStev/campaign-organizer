package com.campaignorganizer.characters.domain.sheet;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A filled-in character sheet, linked to a template and optionally an article/campaign (aggregate root). */
public final class CharacterSheet {

    private final UUID id;
    private final UUID worldId;
    private UUID worldTemplateId;
    private UUID globalTemplateId;
    private UUID articleId;
    private UUID campaignId;
    private String name;
    private Map<String, Object> values;
    private final Instant createdAt;
    private Instant updatedAt;

    private CharacterSheet(UUID id, UUID worldId, UUID worldTemplateId, UUID globalTemplateId,
                           UUID articleId, UUID campaignId, String name, Map<String, Object> values,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(worldTemplateId, globalTemplateId, articleId, campaignId, name, values);
    }

    public static CharacterSheet create(UUID id, UUID worldId, UUID worldTemplateId, UUID globalTemplateId,
                                        UUID articleId, UUID campaignId, String name,
                                        Map<String, Object> values, Instant now) {
        return new CharacterSheet(id, worldId, worldTemplateId, globalTemplateId, articleId, campaignId,
                name, values, now, now);
    }

    public static CharacterSheet reconstitute(UUID id, UUID worldId, UUID worldTemplateId,
                                              UUID globalTemplateId, UUID articleId, UUID campaignId,
                                              String name, Map<String, Object> values, Instant createdAt,
                                              Instant updatedAt) {
        return new CharacterSheet(id, worldId, worldTemplateId, globalTemplateId, articleId, campaignId,
                name, values, createdAt, updatedAt);
    }

    public void update(UUID worldTemplateId, UUID globalTemplateId, UUID articleId, UUID campaignId,
                       String name, Map<String, Object> values, Instant now) {
        apply(worldTemplateId, globalTemplateId, articleId, campaignId, name, values);
        this.updatedAt = now;
    }

    private void apply(UUID worldTemplateId, UUID globalTemplateId, UUID articleId, UUID campaignId,
                       String name, Map<String, Object> values) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Character sheet name must not be blank");
        }
        boolean hasWorld = worldTemplateId != null;
        boolean hasGlobal = globalTemplateId != null;
        if (hasWorld == hasGlobal) {
            throw new ValidationException(
                    "Character sheet requires exactly one of worldTemplateId/globalTemplateId");
        }
        this.worldTemplateId = worldTemplateId;
        this.globalTemplateId = globalTemplateId;
        this.articleId = articleId;
        this.campaignId = campaignId;
        this.name = name;
        this.values = values == null ? new HashMap<>() : values;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getWorldTemplateId() {
        return worldTemplateId;
    }

    public UUID getGlobalTemplateId() {
        return globalTemplateId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
