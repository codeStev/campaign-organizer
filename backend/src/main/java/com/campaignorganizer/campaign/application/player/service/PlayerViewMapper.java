package com.campaignorganizer.campaign.application.player.service;

import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import com.campaignorganizer.campaign.domain.player.Player;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerViewMapper {

    PlayerView toView(Player player);
}
