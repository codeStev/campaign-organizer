package com.campaignorganizer.campaign.application.player.port.in;

import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import java.util.List;
import java.util.UUID;

public interface ListPlayersUseCase {

    List<PlayerView> list(UUID worldId);
}
