package com.campaignorganizer.campaign.application.todo.port.out;

import java.util.UUID;

public interface SessionExistsPort {

    boolean existsInCampaign(UUID sessionId, UUID campaignId);
}
