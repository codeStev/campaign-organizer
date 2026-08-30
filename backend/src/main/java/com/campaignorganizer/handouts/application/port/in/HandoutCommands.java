package com.campaignorganizer.handouts.application.port.in;

import java.util.UUID;

public final class HandoutCommands {

    private HandoutCommands() {
    }

    public record CreateHandoutCommand(UUID worldId, String title, String preset, String body,
                                       UUID sessionId) {
    }

    public record UpdateHandoutCommand(UUID worldId, UUID handoutId, String title,
                                       String preset, String body, UUID sessionId) {
    }
}
