package com.campaignorganizer.handouts.application.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read handout categories from other contexts and aggregates (export). */
public interface HandoutCategoryQueryPort {

    List<HandoutCategoryView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);
}
