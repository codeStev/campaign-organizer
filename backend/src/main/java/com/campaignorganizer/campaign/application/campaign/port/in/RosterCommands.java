package com.campaignorganizer.campaign.application.campaign.port.in;

import java.util.List;
import java.util.UUID;

public final class RosterCommands {

    private RosterCommands() {
    }

    public record RosterEntryInput(UUID playerId, boolean guest) {
    }

    public record SetCampaignRosterCommand(UUID worldId, UUID campaignId, List<RosterEntryInput> entries) {
    }
}
