package com.campaignorganizer.campaign.application.player.port.in;

import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import java.util.UUID;

public interface GetPlayerUseCase {

    PlayerView get(UUID worldId, UUID playerId);
}
