package com.campaignorganizer.tables.domain.carddeck;

import java.util.UUID;

/**
 * One card of a deck (value object): optional face title and Markdown body,
 * which may contain [[wiki-links]] (ADR-0014) resolved at render time.
 */
public record DeckCard(UUID id, String title, String body) {
}
