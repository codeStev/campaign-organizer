package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Persistence model for a roll table (dumb carrier — no logic, no id/time
 * generation; those come from the domain via the mapper). Maps the
 * {@code roll_tables} table.
 */
@Entity
@Table(name = "roll_tables")
public class RollTableJpaEntity {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column
    private String description;

    @Column(name = "dice_expression", nullable = false, length = 100)
    private String diceExpression;

    @Column(name = "min_result", nullable = false)
    private int minResult;

    @Column(name = "max_result", nullable = false)
    private int maxResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<RollTableEntryJson> entries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RollTableJpaEntity() {
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiceExpression() {
        return diceExpression;
    }

    public void setDiceExpression(String diceExpression) {
        this.diceExpression = diceExpression;
    }

    public int getMinResult() {
        return minResult;
    }

    public void setMinResult(int minResult) {
        this.minResult = minResult;
    }

    public int getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public List<RollTableEntryJson> getEntries() {
        return entries;
    }

    public void setEntries(List<RollTableEntryJson> entries) {
        this.entries = entries == null ? new ArrayList<>() : entries;
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
