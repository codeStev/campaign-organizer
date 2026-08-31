package com.campaignorganizer.campaign.adapter.loosethread.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the loose-thread module via the campaign query port. */
@Component
public class LooseThreadCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public LooseThreadCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
