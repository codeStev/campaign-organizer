package com.campaignorganizer.tables.domain.rolltable;

import java.util.List;
import java.util.UUID;

/**
 * One outcome row of a roll table (value object): the inclusive result range it
 * covers (null bounds = unbounded on that side) and its Markdown outcome body,
 * which may contain [[wiki-links]] (ADR-0014) resolved at render time. A row may
 * also chain other tables/decks (FR-41) that resolve after this row comes up;
 * reference cycles are cut at every resolution point, not rejected here.
 */
public record RollTableEntry(UUID id, Integer minResult, Integer maxResult, String body,
                             List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {

    public RollTableEntry {
        nestedTableIds = nestedTableIds == null ? List.of() : List.copyOf(nestedTableIds);
        nestedDeckIds = nestedDeckIds == null ? List.of() : List.copyOf(nestedDeckIds);
    }

    /** Convenience for rows without chained content. */
    public RollTableEntry(UUID id, Integer minResult, Integer maxResult, String body) {
        this(id, minResult, maxResult, body, List.of(), List.of());
    }
}
