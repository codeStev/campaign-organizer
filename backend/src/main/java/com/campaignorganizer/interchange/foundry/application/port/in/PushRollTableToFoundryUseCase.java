package com.campaignorganizer.interchange.foundry.application.port.in;

import com.campaignorganizer.interchange.foundry.application.port.in.PushArticleToFoundryUseCase.FoundryPushResult;
import java.util.UUID;

/** Push a roll table to Foundry as a native {@code RollTable} document (ADR-0115) — unlike an
 * article or handout, this pushes a {@code formula} plus a {@code results[]} array built from
 * each entry, not a single-page JournalEntry. */
public interface PushRollTableToFoundryUseCase {

    /** Named {@code pushRollTable}, not {@code push} — {@link PushArticleToFoundryUseCase}
     * already declares {@code push(UUID, UUID)} with identical erasure; a single implementing
     * class cannot give that one signature two different bodies. */
    FoundryPushResult pushRollTable(UUID worldId, UUID rollTableId);
}
