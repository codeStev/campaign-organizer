package com.campaignorganizer.campaign.application.player.port.in;

import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.CreatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;

public interface CreatePlayerUseCase {

    PlayerView create(CreatePlayerCommand command);
}
