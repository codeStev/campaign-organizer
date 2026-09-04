package com.campaignorganizer.characters.application.category.port.in;

import java.util.UUID;

public final class SheetCategoryCommands {

    private SheetCategoryCommands() {
    }

    public record CreateSheetCategoryCommand(UUID worldId, UUID parentId, String name) {
    }

    public record UpdateSheetCategoryCommand(UUID worldId, UUID categoryId, UUID parentId, String name) {
    }
}
