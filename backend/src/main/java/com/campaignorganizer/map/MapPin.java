package com.campaignorganizer.map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "map_pins")
public class MapPin {

    @Id
    private UUID id;

    @Column(name = "map_id", nullable = false, updatable = false)
    private UUID mapId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(length = 200)
    private String label;

    @Column(length = 100)
    private String layer;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MapPin() {
        // for JPA
    }

    public MapPin(UUID mapId, UUID articleId, String label, String layer, double x, double y) {
        this.mapId = mapId;
        this.articleId = articleId;
        this.label = label;
        this.layer = layer;
        this.x = x;
        this.y = y;
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

    public void update(UUID articleId, String label, String layer, double x, double y) {
        this.articleId = articleId;
        this.label = label;
        this.layer = layer;
        this.x = x;
        this.y = y;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMapId() {
        return mapId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public String getLabel() {
        return label;
    }

    public String getLayer() {
        return layer;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
