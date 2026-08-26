package com.campaignorganizer.campaign.application.session.port.out;

import java.util.UUID;

/** Existence check for cheat-sheet statblock references (FR-37). */
public interface StatblockExistsPort {

    boolean existsInWorld(UUID statblockId, UUID worldId);
}
