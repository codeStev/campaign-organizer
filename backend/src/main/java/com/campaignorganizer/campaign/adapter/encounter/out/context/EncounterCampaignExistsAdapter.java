package com.campaignorganizer.campaign.adapter.encounter.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.encounter.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the encounter module via the campaign query port. */
@Component
public class EncounterCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public EncounterCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
