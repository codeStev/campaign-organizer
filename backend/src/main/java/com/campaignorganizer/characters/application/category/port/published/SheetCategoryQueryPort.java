package com.campaignorganizer.characters.application.category.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read sheet categories from other contexts and aggregates (sheet, statblock, document, template, export). */
public interface SheetCategoryQueryPort {

    List<SheetCategoryView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);
}
