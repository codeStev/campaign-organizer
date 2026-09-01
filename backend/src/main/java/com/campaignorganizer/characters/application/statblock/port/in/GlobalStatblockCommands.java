package com.campaignorganizer.characters.application.statblock.port.in;

import java.util.Map;
import java.util.UUID;

public final class GlobalStatblockCommands {

    private GlobalStatblockCommands() {
    }

    public record CreateGlobalStatblockCommand(UUID systemId, UUID globalTemplateId, String name,
                                                Map<String, Object> stats, String notes) {
    }

    public record UpdateGlobalStatblockCommand(UUID globalStatblockId, UUID systemId, UUID globalTemplateId,
                                                String name, Map<String, Object> stats, String notes) {
    }
}
