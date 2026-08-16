package com.campaignorganizer.characters.application.statblock.port.in;

import java.util.Map;
import java.util.UUID;

public final class StatblockCommands {

    private StatblockCommands() {
    }

    public record CreateStatblockCommand(UUID worldId, UUID articleId, UUID campaignId, String name,
                                         Map<String, Object> stats, String notes) {
    }

    public record UpdateStatblockCommand(UUID worldId, UUID statblockId, UUID articleId, UUID campaignId,
                                         String name, Map<String, Object> stats, String notes) {
    }
}
