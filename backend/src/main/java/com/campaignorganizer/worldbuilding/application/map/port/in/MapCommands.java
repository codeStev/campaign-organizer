package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public final class MapCommands {

    private MapCommands() {
    }

    public record CreateMapCommand(UUID worldId, UUID categoryId, String name, UUID mediaId) {
    }

    public record UpdateMapCommand(UUID worldId, UUID mapId, UUID categoryId, String name, UUID mediaId) {
    }
}
