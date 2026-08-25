package com.campaignorganizer.tables.application.rolltable.port.published;

import java.util.UUID;

/** Published read model of one roll-table entry (raw Markdown body). */
public record RollTableEntryView(UUID id, Integer minResult, Integer maxResult, String body) {
}
