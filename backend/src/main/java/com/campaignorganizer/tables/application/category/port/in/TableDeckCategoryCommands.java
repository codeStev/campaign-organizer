package com.campaignorganizer.tables.application.category.port.in;

import java.util.UUID;

public final class TableDeckCategoryCommands {

    private TableDeckCategoryCommands() {
    }

    public record CreateTableDeckCategoryCommand(UUID worldId, UUID parentId, String name) {
    }

    public record UpdateTableDeckCategoryCommand(UUID worldId, UUID categoryId, UUID parentId, String name) {
    }
}
