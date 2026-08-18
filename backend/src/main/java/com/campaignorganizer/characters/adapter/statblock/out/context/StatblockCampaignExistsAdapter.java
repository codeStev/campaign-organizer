package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the statblock module via the campaign query port. */
@Component
public class StatblockCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public StatblockCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
