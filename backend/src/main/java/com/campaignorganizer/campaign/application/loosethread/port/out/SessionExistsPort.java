package com.campaignorganizer.campaign.application.loosethread.port.out;

import java.util.UUID;

public interface SessionExistsPort {

    boolean existsInCampaign(UUID sessionId, UUID campaignId);
}
