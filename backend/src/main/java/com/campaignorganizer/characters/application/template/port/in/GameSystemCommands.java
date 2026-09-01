package com.campaignorganizer.characters.application.template.port.in;

import java.util.UUID;

public final class GameSystemCommands {

    private GameSystemCommands() {
    }

    public record CreateGameSystemCommand(String name) {
    }

    public record UpdateGameSystemCommand(UUID systemId, String name) {
    }
}
