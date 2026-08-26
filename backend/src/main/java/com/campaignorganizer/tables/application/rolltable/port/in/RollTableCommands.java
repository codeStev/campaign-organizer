package com.campaignorganizer.tables.application.rolltable.port.in;

import java.util.List;
import java.util.UUID;

public final class RollTableCommands {

    private RollTableCommands() {
    }

    public record EntryInput(Integer minResult, Integer maxResult, String body,
                             List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {
    }

    public record CreateRollTableCommand(UUID worldId, String title, String description,
                                         String diceExpression, List<EntryInput> entries) {
    }

    public record UpdateRollTableCommand(UUID worldId, UUID tableId, String title, String description,
                                         String diceExpression, List<EntryInput> entries) {
    }
}
