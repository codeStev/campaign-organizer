package com.campaignorganizer.tables.application.carddeck.port.in;

import java.util.UUID;

public interface DeleteCardDeckUseCase {

    void delete(UUID worldId, UUID deckId);
}
