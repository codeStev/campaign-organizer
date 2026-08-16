package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.campaign.CampaignRepository;
import com.campaignorganizer.characters.application.statblock.port.out.CampaignExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves campaign existence for the statblock module via the legacy campaign repository. */
@Component
public class StatblockCampaignExistsAdapter implements CampaignExistsPort {

    private final CampaignRepository campaigns;

    public StatblockCampaignExistsAdapter(CampaignRepository campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return campaigns.existsByIdAndWorldId(campaignId, worldId);
    }
}
