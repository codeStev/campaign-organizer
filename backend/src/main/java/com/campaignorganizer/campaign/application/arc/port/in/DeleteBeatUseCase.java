package com.campaignorganizer.campaign.application.arc.port.in;

import java.util.UUID;

public interface DeleteBeatUseCase {

    void delete(UUID worldId, UUID campaignId, UUID arcId, UUID beatId);
}
