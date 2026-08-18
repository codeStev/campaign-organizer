package com.campaignorganizer.campaign.application.campaign.port.in;

import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import java.util.List;
import java.util.UUID;

public interface ListCampaignsUseCase {

    List<CampaignView> list(UUID worldId);
}
