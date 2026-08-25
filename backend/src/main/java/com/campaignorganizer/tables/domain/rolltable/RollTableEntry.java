package com.campaignorganizer.tables.domain.rolltable;

import java.util.UUID;

/**
 * One outcome row of a roll table (value object): the inclusive result range it
 * covers (null bounds = unbounded on that side) and its Markdown outcome body,
 * which may contain [[wiki-links]] (ADR-0014) resolved at render time.
 */
public record RollTableEntry(UUID id, Integer minResult, Integer maxResult, String body) {
}
