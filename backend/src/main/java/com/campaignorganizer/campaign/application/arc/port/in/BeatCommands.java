package com.campaignorganizer.campaign.application.arc.port.in;

import java.util.List;
import java.util.UUID;

public final class BeatCommands {

    private BeatCommands() {
    }

    public record CreateBeatCommand(UUID worldId, UUID campaignId, UUID arcId, String title, String body,
                                    boolean done, List<UUID> articleIds, List<UUID> statblockIds,
                                    List<UUID> encounterIds, List<UUID> tableIds, List<UUID> deckIds,
                                    UUID sessionId, Integer position) {
    }

    public record UpdateBeatCommand(UUID worldId, UUID campaignId, UUID arcId, UUID beatId, String title,
                                    String body, boolean done, List<UUID> articleIds,
                                    List<UUID> statblockIds, List<UUID> encounterIds, List<UUID> tableIds,
                                    List<UUID> deckIds, UUID sessionId, Integer position) {
    }
}
