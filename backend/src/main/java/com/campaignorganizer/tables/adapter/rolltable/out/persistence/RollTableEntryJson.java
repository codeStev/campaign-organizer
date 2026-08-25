package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

/** JSON-serialisable entry stored in the roll_tables.entries jsonb column. */
public record RollTableEntryJson(String id, Integer minResult, Integer maxResult, String body) {
}
