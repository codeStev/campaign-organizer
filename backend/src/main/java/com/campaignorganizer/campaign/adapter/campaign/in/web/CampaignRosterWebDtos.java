package com.campaignorganizer.campaign.adapter.campaign.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Web request/response models for the campaign roster (ADR-0091). */
public final class CampaignRosterWebDtos {

    private CampaignRosterWebDtos() {
    }

    public record RosterEntryRequest(@NotNull UUID playerId, boolean guest) {
    }

    public record RosterRequest(@NotNull @Valid List<RosterEntryRequest> entries) {
    }

    public record RosterEntryResponse(UUID playerId, String name, boolean guest) {
    }
}
