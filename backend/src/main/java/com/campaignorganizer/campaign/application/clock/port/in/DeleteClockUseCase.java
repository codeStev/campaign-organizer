package com.campaignorganizer.campaign.application.clock.port.in;

import java.util.UUID;

public interface DeleteClockUseCase {

    void delete(UUID worldId, UUID campaignId, UUID clockId);
}
