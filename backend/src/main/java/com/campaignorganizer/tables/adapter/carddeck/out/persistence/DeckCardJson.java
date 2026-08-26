package com.campaignorganizer.tables.adapter.carddeck.out.persistence;

import java.util.List;

/** JSON-serialisable card stored in the card_decks.cards jsonb column. */
public record DeckCardJson(String id, String title, String body,
                           List<String> nestedTableIds, List<String> nestedDeckIds) {

    /** Compact form for legacy rows written before chaining (FR-41). */
    public DeckCardJson {
        nestedTableIds = nestedTableIds == null ? List.of() : nestedTableIds;
        nestedDeckIds = nestedDeckIds == null ? List.of() : nestedDeckIds;
    }
}
