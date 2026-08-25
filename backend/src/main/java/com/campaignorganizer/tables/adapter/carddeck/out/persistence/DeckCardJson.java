package com.campaignorganizer.tables.adapter.carddeck.out.persistence;

/** JSON-serialisable card stored in the card_decks.cards jsonb column. */
public record DeckCardJson(String id, String title, String body) {
}
