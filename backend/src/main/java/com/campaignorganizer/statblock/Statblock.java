package com.campaignorganizer.statblock;

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
@Table(name = "statblocks")
public class Statblock {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(nullable = false, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> stats = new HashMap<>();

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Statblock() {
        // for JPA
    }

    public Statblock(UUID worldId, UUID articleId, String name, Map<String, Object> stats, String notes) {
        this.worldId = worldId;
        this.articleId = articleId;
        this.name = name;
        this.stats = stats == null ? new HashMap<>() : stats;
        this.notes = notes;
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

    public void update(UUID articleId, String name, Map<String, Object> stats, String notes) {
        this.articleId = articleId;
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
