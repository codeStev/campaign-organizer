package com.campaignorganizer.characters.application.sheet.port.in;

import java.util.Map;
import java.util.UUID;

public final class CharacterSheetCommands {

    private CharacterSheetCommands() {
    }

    public record CreateCharacterSheetCommand(UUID worldId, UUID templateId, UUID articleId,
                                               UUID campaignId, String name, Map<String, Object> values) {
    }

    public record UpdateCharacterSheetCommand(UUID worldId, UUID sheetId, UUID templateId, UUID articleId,
                                               UUID campaignId, String name, Map<String, Object> values) {
    }
}
