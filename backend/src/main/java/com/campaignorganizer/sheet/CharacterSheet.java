package com.campaignorganizer.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "character_sheets")
public class CharacterSheet {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(nullable = false, length = 200)
    private String name;

    /** Field values keyed by the template's field keys, stored as JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_values", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> values = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CharacterSheet() {
        // for JPA
    }

    public CharacterSheet(UUID worldId, UUID templateId, UUID articleId, String name,
                          Map<String, Object> values) {
        this.worldId = worldId;
        this.templateId = templateId;
        this.articleId = articleId;
        this.name = name;
        this.values = values == null ? new HashMap<>() : values;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(UUID templateId, UUID articleId, String name, Map<String, Object> values) {
        this.templateId = templateId;
        this.articleId = articleId;
        this.name = name;
        this.values = values == null ? new HashMap<>() : values;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public UUID getArticleId() {
        return articleId;
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
