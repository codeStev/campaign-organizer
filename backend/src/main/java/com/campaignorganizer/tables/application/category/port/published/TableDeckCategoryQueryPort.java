package com.campaignorganizer.tables.application.category.port.published;

import java.util.List;
import java.util.UUID;

/** Published port: read table/deck categories from other contexts and aggregates (roll table, card deck, export). */
public interface TableDeckCategoryQueryPort {

    List<TableDeckCategoryView> findByWorld(UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);
}
