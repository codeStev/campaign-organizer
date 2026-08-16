package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.UUID;

public final class CategoryCommands {

    private CategoryCommands() {
    }

    public record CreateCategoryCommand(UUID worldId, UUID parentId, String name) {
    }

    public record UpdateCategoryCommand(UUID worldId, UUID categoryId, UUID parentId, String name) {
    }
}
