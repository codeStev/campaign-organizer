package com.campaignorganizer.campaign.adapter.player.in.web;

import com.campaignorganizer.campaign.adapter.player.in.web.PlayerWebDtos.PlayerRequest;
import com.campaignorganizer.campaign.adapter.player.in.web.PlayerWebDtos.PlayerResponse;
import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.CreatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.UpdatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerWebMapper {

    PlayerResponse toResponse(PlayerView view);

    default CreatePlayerCommand toCreateCommand(UUID worldId, PlayerRequest request) {
        return new CreatePlayerCommand(worldId, request.name());
    }

    default UpdatePlayerCommand toUpdateCommand(UUID worldId, UUID playerId, PlayerRequest request) {
        return new UpdatePlayerCommand(worldId, playerId, request.name());
    }
}
