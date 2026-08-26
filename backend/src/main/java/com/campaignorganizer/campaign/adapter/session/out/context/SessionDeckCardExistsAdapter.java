package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.campaign.application.session.port.out.DeckCardExistsPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves deck-card existence for the session module (FR-37). */
@Component
public class SessionDeckCardExistsAdapter implements DeckCardExistsPort {

    private final CardDeckQueryPort decks;

    public SessionDeckCardExistsAdapter(CardDeckQueryPort decks) {
        this.decks = decks;
    }

    @Override
    public boolean cardExistsInWorld(UUID deckId, UUID cardId, UUID worldId) {
        return decks.findByIdInWorld(deckId, worldId)
                .filter(d -> d.cards().stream().anyMatch(c -> c.id().equals(cardId)))
                .isPresent();
    }
}
