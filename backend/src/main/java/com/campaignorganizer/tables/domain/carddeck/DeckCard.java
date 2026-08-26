package com.campaignorganizer.tables.domain.carddeck;

import java.util.List;
import java.util.UUID;

/**
 * One card of a deck (value object): optional face title and Markdown body,
 * which may contain [[wiki-links]] (ADR-0014) resolved at render time. A card
 * may chain other tables/decks (FR-41) that resolve when it is drawn;
 * reference cycles are cut at every resolution point, not rejected here.
 */
public record DeckCard(UUID id, String title, String body,
                       List<UUID> nestedTableIds, List<UUID> nestedDeckIds) {

    public DeckCard {
        nestedTableIds = nestedTableIds == null ? List.of() : List.copyOf(nestedTableIds);
        nestedDeckIds = nestedDeckIds == null ? List.of() : List.copyOf(nestedDeckIds);
    }

    /** Convenience for cards without chained content. */
    public DeckCard(UUID id, String title, String body) {
        this(id, title, body, List.of(), List.of());
    }
}
