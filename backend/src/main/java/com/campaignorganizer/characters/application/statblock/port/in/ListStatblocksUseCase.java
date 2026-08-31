package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import java.util.List;
import java.util.UUID;

public interface ListStatblocksUseCase {

    /**
     * Lists a world's statblocks; when {@code campaignId} is set, scopes to that
     * campaign (ADR-0043); when {@code tag} is set, further restricts to statblocks
     * carrying that tag (ADR-0083).
     */
    List<StatblockView> list(UUID worldId, UUID campaignId, String tag);
}
