package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistence model for a character sheet (maps the {@code character_sheets} table). */
@Entity
@Table(name = "character_sheets")
public class CharacterSheetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "world_template_id")
    private UUID worldTemplateId;

    @Column(name = "global_template_id")
    private UUID globalTemplateId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_values", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> values = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CharacterSheetJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getWorldTemplateId() {
        return worldTemplateId;
    }

    public void setWorldTemplateId(UUID worldTemplateId) {
        this.worldTemplateId = worldTemplateId;
    }

    public UUID getGlobalTemplateId() {
        return globalTemplateId;
    }

    public void setGlobalTemplateId(UUID globalTemplateId) {
        this.globalTemplateId = globalTemplateId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public void setArticleId(UUID articleId) {
        this.articleId = articleId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values == null ? new HashMap<>() : values;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
