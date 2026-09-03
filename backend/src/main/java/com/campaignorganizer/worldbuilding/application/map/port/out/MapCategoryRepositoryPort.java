package com.campaignorganizer.worldbuilding.application.map.port.out;

import com.campaignorganizer.worldbuilding.domain.map.MapCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MapCategoryRepositoryPort {

    List<MapCategory> findByWorldOrderByName(UUID worldId);

    Optional<MapCategory> findByIdAndWorld(UUID categoryId, UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);

    MapCategory save(MapCategory category);

    void delete(MapCategory category);
}
