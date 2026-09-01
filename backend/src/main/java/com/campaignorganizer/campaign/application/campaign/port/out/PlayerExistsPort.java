package com.campaignorganizer.campaign.application.campaign.port.out;

import java.util.Optional;
import java.util.UUID;

public interface PlayerExistsPort {

    boolean existsInWorld(UUID playerId, UUID worldId);

    Optional<String> findName(UUID playerId, UUID worldId);
}
