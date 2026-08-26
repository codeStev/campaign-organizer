package com.campaignorganizer.campaign.application.session.port.out;

import java.util.UUID;

/** Existence check for a specific roll-table row on a cheat sheet (FR-37). */
public interface TableEntryExistsPort {

    boolean entryExistsInWorld(UUID tableId, UUID entryId, UUID worldId);
}
