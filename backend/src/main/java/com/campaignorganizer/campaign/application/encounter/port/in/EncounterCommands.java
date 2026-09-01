package com.campaignorganizer.campaign.application.encounter.port.in;

import java.util.List;
import java.util.UUID;

public final class EncounterCommands {

    private EncounterCommands() {
    }

    public record EntryInput(UUID statblockId, int quantity) {
    }

    public record CreateEncounterCommand(UUID worldId, UUID campaignId, String name, String notes,
                                         List<EntryInput> entries) {
    }

    public record UpdateEncounterCommand(UUID worldId, UUID campaignId, UUID encounterId, String name,
                                         String notes, List<EntryInput> entries) {
    }
}
