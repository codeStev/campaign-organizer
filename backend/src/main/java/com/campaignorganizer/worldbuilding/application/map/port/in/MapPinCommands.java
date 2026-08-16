package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public final class MapPinCommands {

    private MapPinCommands() {
    }

    public record CreateMapPinCommand(UUID worldId, UUID mapId, UUID articleId, String label,
                                      String layer, double x, double y) {
    }

    public record UpdateMapPinCommand(UUID worldId, UUID mapId, UUID pinId, UUID articleId, String label,
                                      String layer, double x, double y) {
    }
}
