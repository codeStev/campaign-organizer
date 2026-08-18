package com.campaignorganizer.campaign.adapter.arc.out.context;

import com.campaignorganizer.campaign.application.arc.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the arc module via the campaign query port. */
@Component
public class ArcCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public ArcCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
