package com.campaignorganizer.tables.application.carddeck.port.published;

import java.util.UUID;

/** Published read model of one deck card (raw Markdown body). */
public record DeckCardView(UUID id, String title, String body) {
}
