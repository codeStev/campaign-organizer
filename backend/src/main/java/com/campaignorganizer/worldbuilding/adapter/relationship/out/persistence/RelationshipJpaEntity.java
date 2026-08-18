package com.campaignorganizer.worldbuilding.adapter.relationship.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a relationship (maps the {@code relationships} table). */
@Entity
@Table(name = "relationships")
public class RelationshipJpaEntity {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "from_article_id", nullable = false)
    private UUID fromArticleId;

    @Column(name = "to_article_id", nullable = false)
    private UUID toArticleId;

    @Column(length = 100)
    private String label;

    @Column(nullable = false)
    private boolean directed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RelationshipJpaEntity() {
        // for JPA
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

    public UUID getFromArticleId() {
        return fromArticleId;
    }

    public void setFromArticleId(UUID fromArticleId) {
        this.fromArticleId = fromArticleId;
    }

    public UUID getToArticleId() {
        return toArticleId;
    }

    public void setToArticleId(UUID toArticleId) {
        this.toArticleId = toArticleId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDirected() {
        return directed;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
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
