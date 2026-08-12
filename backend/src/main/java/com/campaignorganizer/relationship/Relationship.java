package com.campaignorganizer.relationship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "relationships")
public class Relationship {

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

    protected Relationship() {
        // for JPA
    }

    public Relationship(UUID worldId, UUID fromArticleId, UUID toArticleId, String label, boolean directed) {
        this.worldId = worldId;
        this.fromArticleId = fromArticleId;
        this.toArticleId = toArticleId;
        this.label = label;
        this.directed = directed;
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

    public void update(UUID fromArticleId, UUID toArticleId, String label, boolean directed) {
        this.fromArticleId = fromArticleId;
        this.toArticleId = toArticleId;
        this.label = label;
        this.directed = directed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getFromArticleId() {
        return fromArticleId;
    }

    public UUID getToArticleId() {
        return toArticleId;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDirected() {
        return directed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
