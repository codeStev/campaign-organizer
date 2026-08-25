package com.campaignorganizer.tables.application.rolltable.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model of a roll table (with its raw entry bodies). */
public record RollTableView(
        UUID id,
        UUID worldId,
        String title,
        String description,
        String diceExpression,
        int minResult,
        int maxResult,
        List<RollTableEntryView> entries,
        Instant createdAt,
        Instant updatedAt) {
}
