package com.campaignorganizer.campaign.application.arc.port.out;

import java.util.UUID;

public interface CampaignExistsPort {

    boolean existsInWorld(UUID campaignId, UUID worldId);
}
