package com.campaignorganizer.campaign.application.beatkind.port.in;

import java.util.UUID;

public final class BeatKindCommands {

    private BeatKindCommands() {
    }

    public record CreateBeatKindCommand(UUID worldId, String name, String color) {
    }

    public record UpdateBeatKindCommand(UUID worldId, UUID beatKindId, String name, String color) {
    }
}
