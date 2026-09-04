package com.campaignorganizer.worldbuilding.application.world.port.in;

import java.util.UUID;

public final class WorldCommands {

    private WorldCommands() {
    }

    public record CreateWorldCommand(String name, String description, boolean scratch) {
    }

    public record UpdateWorldCommand(UUID worldId, String name, String description, boolean scratch) {
    }
}
