package com.campaignorganizer.campaign.application.player.port.in;

import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.UpdatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;

public interface UpdatePlayerUseCase {

    PlayerView update(UpdatePlayerCommand command);
}
