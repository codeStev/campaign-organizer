package com.campaignorganizer.campaign.application.arc.port.out;

import java.util.UUID;

/** Checks that a roll table exists in the given world (implemented via the tables context). */
public interface TableExistsPort {

    boolean existsInWorld(UUID tableId, UUID worldId);
}
