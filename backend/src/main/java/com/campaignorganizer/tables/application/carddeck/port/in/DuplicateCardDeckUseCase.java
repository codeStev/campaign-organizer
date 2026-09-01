package com.campaignorganizer.tables.application.carddeck.port.in;

import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import java.util.UUID;

public interface DuplicateCardDeckUseCase {

    CardDeckView duplicate(UUID worldId, UUID deckId);
}
