package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read categories from other contexts and aggregates (article, export). */
public interface CategoryQueryPort {

    List<CategoryView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);
}
