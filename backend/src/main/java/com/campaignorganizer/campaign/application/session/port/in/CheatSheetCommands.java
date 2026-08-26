package com.campaignorganizer.campaign.application.session.port.in;

import java.util.List;
import java.util.UUID;

public final class CheatSheetCommands {

    private CheatSheetCommands() {
    }

    public record FragmentInput(String type, String text, UUID statblockId, UUID tableId,
                                UUID entryId, UUID deckId, UUID cardId) {
    }

    public record PutCheatSheetCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                       List<FragmentInput> fragments) {
    }
}
