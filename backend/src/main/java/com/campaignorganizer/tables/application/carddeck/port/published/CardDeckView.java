package com.campaignorganizer.tables.application.carddeck.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model of a card deck (with its raw card bodies). */
public record CardDeckView(
        UUID id,
        UUID worldId,
        UUID categoryId,
        String title,
        String description,
        List<DeckCardView> cards,
        Instant createdAt,
        Instant updatedAt) {
}
