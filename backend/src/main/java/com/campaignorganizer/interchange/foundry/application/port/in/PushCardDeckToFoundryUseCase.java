package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import java.util.UUID;

/** Push a card deck to Foundry as a native {@code Cards} document (ADR-0115) — one embedded
 * {@code Card} per {@code DeckCard}, not a single-page JournalEntry. */
public interface PushCardDeckToFoundryUseCase {

    /** Named {@code pushCardDeck}, not {@code push} — {@link PushArticleToFoundryUseCase}
     * already declares {@code push(UUID, UUID)} with identical erasure; a single implementing
     * class cannot give that one signature two different bodies. */
    FoundryPushResult pushCardDeck(UUID worldId, UUID cardDeckId);
}
