package com.campaignorganizer.campaign.application.beatkind.port.in;

import java.util.UUID;

public interface DeleteBeatKindUseCase {

    void delete(UUID worldId, UUID beatKindId);
}
