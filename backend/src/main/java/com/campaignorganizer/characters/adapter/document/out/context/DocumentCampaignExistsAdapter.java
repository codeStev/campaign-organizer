package com.campaignorganizer.characters.adapter.document.out.context;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.characters.application.document.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the document module via the campaign query port. */
@Component
public class DocumentCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignQueryPort campaigns;

    public DocumentCampaignExistsAdapter(CampaignQueryPort campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsInWorld(campaignId, worldId);
    }
}
