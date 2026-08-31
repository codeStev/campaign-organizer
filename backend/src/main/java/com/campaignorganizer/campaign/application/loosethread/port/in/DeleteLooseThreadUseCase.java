package com.campaignorganizer.campaign.application.loosethread.port.in;

import java.util.UUID;

public interface DeleteLooseThreadUseCase {

    void delete(UUID worldId, UUID campaignId, UUID sessionId, UUID threadId);
}
