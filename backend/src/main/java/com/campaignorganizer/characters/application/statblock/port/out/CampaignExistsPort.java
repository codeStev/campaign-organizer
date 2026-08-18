package com.campaignorganizer.characters.application.statblock.port.out;

import java.util.UUID;

public interface CampaignExistsPort {

    boolean existsInWorld(UUID campaignId, UUID worldId);
}
