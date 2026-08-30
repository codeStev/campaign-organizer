package com.campaignorganizer.handouts.application.port.in;

import java.util.List;
import java.util.UUID;

public final class HandoutCommands {

    private HandoutCommands() {
    }

    public record CreateHandoutCommand(UUID worldId, String title, String preset, String body,
                                       UUID sessionId, boolean revealed) {
    }

    public record UpdateHandoutCommand(UUID worldId, UUID handoutId, String title,
                                       String preset, String body, UUID sessionId,
                                       boolean revealed) {
    }

    /** orderedIds must be exactly the world's current handout ids, in the new order. */
    public record ReorderHandoutsCommand(UUID worldId, List<UUID> orderedIds) {
    }
}
