package com.campaignorganizer.campaign.application.player.port.in;

import java.util.UUID;

public interface DeletePlayerUseCase {

    void delete(UUID worldId, UUID playerId);
}
