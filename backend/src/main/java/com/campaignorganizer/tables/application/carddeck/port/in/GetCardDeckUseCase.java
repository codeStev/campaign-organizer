package com.campaignorganizer.tables.application.carddeck.port.in;

import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import java.util.UUID;

public interface GetCardDeckUseCase {

    CardDeckView get(UUID worldId, UUID deckId);
}
