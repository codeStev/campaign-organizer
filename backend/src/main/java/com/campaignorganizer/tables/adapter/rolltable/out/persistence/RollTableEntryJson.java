package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

import java.util.List;

/** JSON-serialisable entry stored in the roll_tables.entries jsonb column. */
public record RollTableEntryJson(String id, Integer minResult, Integer maxResult, String body,
                                 List<String> nestedTableIds, List<String> nestedDeckIds) {

    /** Compact form for legacy rows written before chaining (FR-41). */
    public RollTableEntryJson {
        nestedTableIds = nestedTableIds == null ? List.of() : nestedTableIds;
        nestedDeckIds = nestedDeckIds == null ? List.of() : nestedDeckIds;
    }
}
