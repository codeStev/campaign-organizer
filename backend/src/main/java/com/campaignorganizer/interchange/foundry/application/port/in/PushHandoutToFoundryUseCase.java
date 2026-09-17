package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import java.util.UUID;

/** Push a handout to Foundry as a JournalEntry (ADR-0115) — unlike an article, a handout's
 * body has no wiki-link rendering step and is pushed completely verbatim. */
public interface PushHandoutToFoundryUseCase {

    /** Named {@code pushHandout}, not {@code push} — {@link PushArticleToFoundryUseCase} already
     * declares {@code push(UUID, UUID)} with identical erasure; a single implementing class
     * cannot give that one signature two different bodies. */
    FoundryPushResult pushHandout(UUID worldId, UUID handoutId);
}
