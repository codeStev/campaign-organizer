package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.session.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the session module via the campaign query port. */
@Component
public class SessionCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public SessionCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
