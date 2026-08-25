package com.campaignorganizer.campaign.adapter.arc.out.context;

import com.campaignorganizer.campaign.application.arc.port.out.DeckExistsPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves card-deck existence for the beat module via the tables query port. */
@Component
public class BeatDeckExistsAdapter implements DeckExistsPort {

    private final CardDeckQueryPort decks;

    public BeatDeckExistsAdapter(CardDeckQueryPort decks) {
        this.decks = decks;
    }

    @Override
    public boolean existsInWorld(UUID deckId, UUID worldId) {
        return decks.existsInWorld(deckId, worldId);
    }
}
