package com.campaignorganizer.campaign.application.campaign.port.in;

import java.util.UUID;

public final class CampaignCommands {

    private CampaignCommands() {
    }

    public record CreateCampaignCommand(UUID worldId, String name, String description, String notes) {
    }

    public record UpdateCampaignCommand(UUID worldId, UUID campaignId, String name, String description,
                                        String notes) {
    }
}
