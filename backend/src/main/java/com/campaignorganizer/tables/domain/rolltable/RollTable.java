package com.campaignorganizer.tables.domain.rolltable;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * A random table keyed on a dice expression: rolling it picks the entry whose
 * result range covers the total (aggregate root, FR-40). Entries are value
 * objects; explicit ranges must sit inside the expression's {@code [min..max]}
 * and may not overlap — gaps are allowed (a rolled gap yields no entry), unless
 * one catch-all entry (no bounds) claims every remaining result.
 */
public final class RollTable {

    private final UUID id;
    private final UUID worldId;
    private String title;
    private String description;
    private String diceExpression;
    private int minResult;
    private int maxResult;
    private List<RollTableEntry> entries;
    private final Instant createdAt;
    private Instant updatedAt;

    private RollTable(UUID id, UUID worldId, String title, String description, String diceExpression,
                      int minResult, int maxResult, List<RollTableEntry> entries,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, description, diceExpression, minResult, maxResult, entries);
    }

    public static RollTable create(UUID id, UUID worldId, String title, String description,
                                   String diceExpression, List<RollTableEntry> entries, Instant now) {
        DiceExpression.Range range = DiceExpression.range(diceExpression);
        return new RollTable(id, worldId, title, description, diceExpression,
                range.min(), range.max(), entries, now, now);
    }

    public static RollTable reconstitute(UUID id, UUID worldId, String title, String description,
                                         String diceExpression, int minResult, int maxResult,
                                         List<RollTableEntry> entries, Instant createdAt,
                                         Instant updatedAt) {
        return new RollTable(id, worldId, title, description, diceExpression, minResult, maxResult,
                entries, createdAt, updatedAt);
    }

    public void update(String title, String description, String diceExpression,
                       List<RollTableEntry> entries, Instant now) {
        DiceExpression.Range range = DiceExpression.range(diceExpression);
        apply(title, description, diceExpression, range.min(), range.max(), entries);
        this.updatedAt = now;
    }

    private void apply(String title, String description, String diceExpression, int minResult,
                       int maxResult, List<RollTableEntry> entries) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Roll table title must not be blank");
        }
        if (title.length() > 200) {
            throw new ValidationException("Roll table title must not exceed 200 characters");
        }
        List<RollTableEntry> list = entries == null ? List.of() : List.copyOf(entries);
        validateEntries(minResult, maxResult, list);
        this.title = title;
        this.description = description;
        this.diceExpression = diceExpression;
        this.minResult = minResult;
        this.maxResult = maxResult;
        this.entries = list;
    }

    private static void validateEntries(int minResult, int maxResult, List<RollTableEntry> entries) {
        record Bound(int low, int high) {
        }
        List<Bound> bounds = new ArrayList<>();
        int fallbacks = 0;
        for (RollTableEntry entry : entries) {
            if (entry.minResult() == null && entry.maxResult() == null) {
                // The catch-all row covering every result no explicit entry claims.
                fallbacks++;
                continue;
            }
            if (entry.minResult() == null || entry.maxResult() == null) {
                throw new ValidationException(
                        "Entry needs both result bounds or neither");
            }
            if (entry.minResult() < minResult || entry.minResult() > maxResult
                    || entry.maxResult() < minResult || entry.maxResult() > maxResult) {
                throw new ValidationException(
                        "Entry range lies outside the dice expression's result range");
            }
            if (entry.minResult() > entry.maxResult()) {
                throw new ValidationException("Entry result range is inverted");
            }
            bounds.add(new Bound(entry.minResult(), entry.maxResult()));
        }
        if (fallbacks > 1) {
            throw new ValidationException(
                    "Only one entry may cover the remaining results");
        }
        bounds.sort(Comparator.comparingInt(Bound::low));
        for (int i = 1; i < bounds.size(); i++) {
            if (bounds.get(i).low() <= bounds.get(i - 1).high()) {
                throw new ValidationException("Entry result ranges must not overlap");
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDiceExpression() {
        return diceExpression;
    }

    public int getMinResult() {
        return minResult;
    }

    public int getMaxResult() {
        return maxResult;
    }

    public List<RollTableEntry> getEntries() {
        return entries;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
