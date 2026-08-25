package com.campaignorganizer.tables.application.carddeck.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Published port for other contexts (beat validation, session packet, usage,
 * export). Exposes card-deck reads without leaking domain/persistence.
 */
public interface CardDeckQueryPort {

    boolean existsInWorld(UUID deckId, UUID worldId);

    Optional<CardDeckView> findByIdInWorld(UUID deckId, UUID worldId);

    Optional<CardDeckView> findById(UUID deckId);

    List<CardDeckView> findByWorld(UUID worldId);
}
