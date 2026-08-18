package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read maps from other contexts and aggregates (usage, export, packet). */
public interface MapQueryPort {

    List<MapView> findByWorld(UUID worldId);

    Optional<MapView> findByIdInWorld(UUID mapId, UUID worldId);

    Optional<MapView> findById(UUID mapId);
}
