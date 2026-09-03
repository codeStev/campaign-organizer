package com.campaignorganizer.characters.application.statblock.port.in;

import java.util.Map;
import java.util.UUID;

public final class StatblockCommands {

    private StatblockCommands() {
    }

    public record CreateStatblockCommand(UUID worldId, UUID categoryId, UUID articleId, UUID campaignId,
                                         UUID worldTemplateId, UUID globalTemplateId, String name,
                                         Map<String, Object> stats, String notes) {
    }

    public record UpdateStatblockCommand(UUID worldId, UUID statblockId, UUID categoryId, UUID articleId,
                                         UUID campaignId, UUID worldTemplateId, UUID globalTemplateId,
                                         String name, Map<String, Object> stats, String notes) {
    }
}
