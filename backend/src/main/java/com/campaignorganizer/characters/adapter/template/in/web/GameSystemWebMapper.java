package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.GameSystemWebDtos.GameSystemRequest;
import com.campaignorganizer.characters.adapter.template.in.web.GameSystemWebDtos.GameSystemResponse;
import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.CreateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.in.GameSystemCommands.UpdateGameSystemCommand;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GameSystemWebMapper {

    GameSystemResponse toResponse(GameSystemView view);

    default CreateGameSystemCommand toCreateCommand(GameSystemRequest request) {
        return new CreateGameSystemCommand(request.name(), request.tagline(), request.color(),
                request.notes());
    }

    default UpdateGameSystemCommand toUpdateCommand(UUID systemId, GameSystemRequest request) {
        return new UpdateGameSystemCommand(systemId, request.name(), request.tagline(), request.color(),
                request.notes());
    }
}
