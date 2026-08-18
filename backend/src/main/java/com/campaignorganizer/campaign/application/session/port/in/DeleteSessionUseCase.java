package com.campaignorganizer.campaign.application.session.port.in;

import java.util.UUID;

public interface DeleteSessionUseCase {

    void delete(UUID worldId, UUID campaignId, UUID sessionId);
}
