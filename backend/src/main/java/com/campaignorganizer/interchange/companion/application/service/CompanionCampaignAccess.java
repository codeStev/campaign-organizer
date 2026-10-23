package com.campaignorganizer.interchange.companion.application.service;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The one ownership check of the companion API (ADR-0120): a campaign counts only for the account
 * that owns its world, and anything else, including an unknown id, is simply not found (ADR-0109).
 * Deliberately explicit rather than left to row-level security alone.
 */
@Component
public class CompanionCampaignAccess {

    private final CampaignQueryPort campaigns;
    private final WorldQueryPort worlds;

    public CompanionCampaignAccess(CampaignQueryPort campaigns, WorldQueryPort worlds) {
        this.campaigns = campaigns;
        this.worlds = worlds;
    }

    public CampaignView requireOwned(UUID accountId, UUID campaignId) {
        CampaignView campaign = campaigns.findById(campaignId).orElseThrow(CompanionCampaignAccess::notFound);
        worlds.findById(campaign.worldId()).filter(world -> accountId.equals(world.ownerId()))
                .orElseThrow(CompanionCampaignAccess::notFound);
        return campaign;
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Campaign not found");
    }
}
