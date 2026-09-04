package com.campaignorganizer.worldbuilding.application.map.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read map categories from other contexts and aggregates (map, export). */
public interface MapCategoryQueryPort {

    List<MapCategoryView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);
}
