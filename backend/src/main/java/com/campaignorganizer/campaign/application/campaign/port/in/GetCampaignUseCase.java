package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import java.util.UUID;

public interface GetCampaignUseCase {

    CampaignView get(UUID worldId, UUID campaignId);
}
