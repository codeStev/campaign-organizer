package com.campaignorganizer.campaign.application.arc.port.out;

import java.util.UUID;

/** Checks that a card deck exists in the given world (implemented via the tables context). */
public interface DeckExistsPort {

    boolean existsInWorld(UUID deckId, UUID worldId);
}
