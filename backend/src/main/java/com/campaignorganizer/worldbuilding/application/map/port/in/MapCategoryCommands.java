package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public final class MapCategoryCommands {

    private MapCategoryCommands() {
    }

    public record CreateMapCategoryCommand(UUID worldId, UUID parentId, String name) {
    }

    public record UpdateMapCategoryCommand(UUID worldId, UUID categoryId, UUID parentId, String name) {
    }
}
