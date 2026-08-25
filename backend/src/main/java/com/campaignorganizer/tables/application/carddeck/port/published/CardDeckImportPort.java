package com.campaignorganizer.tables.application.carddeck.port.published;

/** Published import for backup/restore (ADR-0061): saves a deck verbatim. */
public interface CardDeckImportPort {

    CardDeckView importCardDeck(CardDeckView view);
}
