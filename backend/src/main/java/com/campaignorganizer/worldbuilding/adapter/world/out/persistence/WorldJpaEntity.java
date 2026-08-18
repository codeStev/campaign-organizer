package com.campaignorganizer.worldbuilding.adapter.world.out.persistence;

import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
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

/** Persistence model for a world (maps the {@code worlds} table). */
@Entity
@Table(name = "worlds")
public class WorldJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    /** Per-layer map styling (colour + icon), keyed by layer name (ADR-0049). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layer_styles", nullable = false, columnDefinition = "jsonb")
    private Map<String, LayerStyle> layerStyles = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorldJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, LayerStyle> getLayerStyles() {
        return layerStyles;
    }

    public void setLayerStyles(Map<String, LayerStyle> layerStyles) {
        this.layerStyles = layerStyles == null ? new HashMap<>() : layerStyles;
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
