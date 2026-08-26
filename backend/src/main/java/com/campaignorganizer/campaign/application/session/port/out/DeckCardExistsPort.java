package com.campaignorganizer.campaign.application.session.port.out;

import java.util.UUID;

/** Existence check for a specific deck card on a cheat sheet (FR-37). */
public interface DeckCardExistsPort {

    boolean cardExistsInWorld(UUID deckId, UUID cardId, UUID worldId);
}
