package com.campaignorganizer.campaign.application.player.port.in;

import java.util.UUID;

public final class PlayerCommands {

    private PlayerCommands() {
    }

    public record CreatePlayerCommand(UUID worldId, String name) {
    }

    public record UpdatePlayerCommand(UUID worldId, UUID playerId, String name) {
    }
}
