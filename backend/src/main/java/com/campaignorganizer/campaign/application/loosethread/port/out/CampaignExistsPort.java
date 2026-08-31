package com.campaignorganizer.campaign.application.loosethread.port.out;

import java.util.UUID;

public interface CampaignExistsPort {

    boolean existsInWorld(UUID campaignId, UUID worldId);
}
