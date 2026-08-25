package com.campaignorganizer.tables.application.carddeck.port.out;

import com.campaignorganizer.tables.domain.carddeck.CardDeck;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardDeckRepositoryPort {

    List<CardDeck> findByWorld(UUID worldId);

    Optional<CardDeck> findByIdAndWorld(UUID deckId, UUID worldId);

    Optional<CardDeck> findById(UUID deckId);

    boolean existsInWorld(UUID deckId, UUID worldId);

    CardDeck save(CardDeck deck);

    void delete(CardDeck deck);
}
