package com.campaignorganizer.campaign.application.campaign.port.in;

import java.util.UUID;

public interface DeleteCampaignUseCase {

    void delete(UUID worldId, UUID campaignId);
}
