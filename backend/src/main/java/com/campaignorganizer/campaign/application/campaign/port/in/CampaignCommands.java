package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import java.util.UUID;

public final class CampaignCommands {

    private CampaignCommands() {
    }

    public record CreateCampaignCommand(UUID worldId, String name, String description, String notes,
                                        CampaignStatus status, UUID systemId, String color) {
    }

    public record UpdateCampaignCommand(UUID worldId, UUID campaignId, String name, String description,
                                        String notes, CampaignStatus status, UUID systemId, String color) {
    }
}
