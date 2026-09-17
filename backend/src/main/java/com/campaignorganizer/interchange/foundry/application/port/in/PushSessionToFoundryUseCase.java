package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.List;
import java.util.UUID;

/** Push everything a prepped session needs (ADR-0115) — the same content the existing
 * "print session packet" feature (ADR-0036) already discovers via beats: referenced
 * articles, session handouts, and any roll tables/card decks the beats chain to. Maps
 * and statblocks are deliberately excluded (maps: exported from Dungeondraft directly;
 * statblocks: needs the still-deferred per-game-system Actor mapper), permanently — not
 * a temporary gap. */
public interface PushSessionToFoundryUseCase {

    FoundrySessionPushResult pushSession(UUID worldId, UUID campaignId, UUID sessionId);

    /** Per-entity-type counts plus every warning collected across all the individual
     * pushes this ran (e.g. an oversized embedded image skipped on one of the
     * articles) — one flat list, since the UI shows them as a single summary rather
     * than needing to trace a warning back to which specific entity produced it. */
    record FoundrySessionPushResult(int articlesPushed, int handoutsPushed, int rollTablesPushed,
                                    int cardDecksPushed, List<String> warnings) {
    }
}
