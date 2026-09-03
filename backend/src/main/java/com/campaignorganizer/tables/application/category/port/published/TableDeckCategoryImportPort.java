package com.campaignorganizer.tables.application.category.port.published;

/**
 * Published port: persists a table/deck category exactly as given (id and
 * foreign keys already resolved by the caller) instead of generating a new
 * id — backup import's counterpart to the normal create flow (ADR-0061).
 */
public interface TableDeckCategoryImportPort {

    TableDeckCategoryView importTableDeckCategory(TableDeckCategoryView view);
}
