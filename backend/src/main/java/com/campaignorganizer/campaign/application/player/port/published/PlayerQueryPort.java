package com.campaignorganizer.campaign.application.player.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read players from sibling aggregates (roster, attendance). */
public interface PlayerQueryPort {

    List<PlayerView> findByWorld(UUID worldId);

    Optional<PlayerView> findByIdInWorld(UUID playerId, UUID worldId);

    boolean existsInWorld(UUID playerId, UUID worldId);
}
