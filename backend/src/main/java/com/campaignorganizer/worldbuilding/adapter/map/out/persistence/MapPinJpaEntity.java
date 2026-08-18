package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a map pin (maps the {@code map_pins} table). */
@Entity
@Table(name = "map_pins")
public class MapPinJpaEntity {

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

    protected MapPinJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMapId() {
        return mapId;
    }

    public void setMapId(UUID mapId) {
        this.mapId = mapId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public void setArticleId(UUID articleId) {
        this.articleId = articleId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
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
