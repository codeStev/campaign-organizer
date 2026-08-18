package com.campaignorganizer.campaign.application.arc.port.out;

import java.util.UUID;

public interface StatblockExistsPort {

    boolean existsInWorld(UUID statblockId, UUID worldId);
}
