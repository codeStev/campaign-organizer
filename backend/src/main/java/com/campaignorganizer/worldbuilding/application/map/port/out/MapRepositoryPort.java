package com.campaignorganizer.worldbuilding.application.map.port.out;

import com.campaignorganizer.worldbuilding.domain.map.WorldMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MapRepositoryPort {

    List<WorldMap> findByWorld(UUID worldId);

    Optional<WorldMap> findByIdAndWorld(UUID mapId, UUID worldId);

    Optional<WorldMap> findById(UUID mapId);

    WorldMap save(WorldMap map);

    void delete(WorldMap map);
}
