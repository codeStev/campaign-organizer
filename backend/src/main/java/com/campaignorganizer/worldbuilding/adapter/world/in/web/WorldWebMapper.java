package com.campaignorganizer.worldbuilding.adapter.world.in.web;

import com.campaignorganizer.worldbuilding.adapter.world.in.web.WorldWebDtos.WorldRequest;
import com.campaignorganizer.worldbuilding.adapter.world.in.web.WorldWebDtos.WorldResponse;
import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.CreateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.UpdateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorldWebMapper {

    WorldResponse toResponse(WorldView view);

    default CreateWorldCommand toCreateCommand(WorldRequest request) {
        return new CreateWorldCommand(request.name(), request.description());
    }

    default UpdateWorldCommand toUpdateCommand(UUID worldId, WorldRequest request) {
        return new UpdateWorldCommand(worldId, request.name(), request.description());
    }
}
