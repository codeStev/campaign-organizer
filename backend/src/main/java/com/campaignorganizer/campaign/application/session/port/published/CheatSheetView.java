package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model of a session cheat sheet (FR-37). */
public record CheatSheetView(UUID id, UUID sessionId, List<FragmentView> fragments,
                             Instant createdAt, Instant updatedAt) {

    /** {@code id == null} means no sheet has been saved for the session yet. */
    public record FragmentView(UUID id, String type, String text, UUID statblockId,
                               UUID tableId, UUID entryId, UUID deckId, UUID cardId) {
    }
}
