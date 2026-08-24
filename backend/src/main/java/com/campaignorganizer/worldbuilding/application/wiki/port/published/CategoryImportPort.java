package com.campaignorganizer.worldbuilding.application.wiki.port.published;

/**
 * Published port: persists a category exactly as given (id and foreign keys
 * already resolved by the caller) instead of generating a new id — backup
 * import's counterpart to the normal create flow (ADR-0061).
 */
public interface CategoryImportPort {

    CategoryView importCategory(CategoryView view);
}
