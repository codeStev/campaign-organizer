package com.campaignorganizer.interchange.overview.application.port.in;

import com.campaignorganizer.interchange.overview.application.port.in.CampaignOverviewDtos.CampaignOverviewStats;
import java.util.UUID;

public interface GetCampaignOverviewUseCase {

    CampaignOverviewStats overview(UUID worldId, UUID campaignId);
}
