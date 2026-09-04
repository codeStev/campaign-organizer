package com.campaignorganizer.handouts.application.port.in;

import java.util.UUID;

public final class HandoutCategoryCommands {

    private HandoutCategoryCommands() {
    }

    public record CreateHandoutCategoryCommand(UUID worldId, UUID parentId, String name) {
    }

    public record UpdateHandoutCategoryCommand(UUID worldId, UUID categoryId, UUID parentId, String name) {
    }
}
