package com.campaignorganizer.campaign.application.session.port.out;

import java.util.UUID;

public interface CampaignExistsPort {

    boolean existsInWorld(UUID campaignId, UUID worldId);
}
