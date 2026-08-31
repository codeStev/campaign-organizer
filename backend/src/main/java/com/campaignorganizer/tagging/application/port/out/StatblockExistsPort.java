package com.campaignorganizer.tagging.application.port.out;

import java.util.UUID;

public interface StatblockExistsPort {

    boolean existsInWorld(UUID statblockId, UUID worldId);
}
