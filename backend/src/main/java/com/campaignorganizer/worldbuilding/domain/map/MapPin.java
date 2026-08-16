package com.campaignorganizer.worldbuilding.domain.map;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A pin placed on a map by fractional coordinates (aggregate root). */
public final class MapPin {

    private final UUID id;
    private final UUID mapId;
    private UUID articleId;
    private String label;
    private String layer;
    private double x;
    private double y;
    private final Instant createdAt;
    private Instant updatedAt;

    private MapPin(UUID id, UUID mapId, UUID articleId, String label, String layer, double x, double y,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.mapId = mapId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleId, label, layer, x, y);
    }

    public static MapPin create(UUID id, UUID mapId, UUID articleId, String label, String layer,
                                double x, double y, Instant now) {
        return new MapPin(id, mapId, articleId, label, layer, x, y, now, now);
    }

    public static MapPin reconstitute(UUID id, UUID mapId, UUID articleId, String label, String layer,
                                      double x, double y, Instant createdAt, Instant updatedAt) {
        return new MapPin(id, mapId, articleId, label, layer, x, y, createdAt, updatedAt);
    }

    public void update(UUID articleId, String label, String layer, double x, double y, Instant now) {
        apply(articleId, label, layer, x, y);
        this.updatedAt = now;
    }

    private void apply(UUID articleId, String label, String layer, double x, double y) {
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            throw new ValidationException("Pin coordinates must be within [0, 1]");
        }
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
