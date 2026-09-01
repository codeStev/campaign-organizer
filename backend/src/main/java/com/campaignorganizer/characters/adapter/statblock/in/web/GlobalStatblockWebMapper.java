package com.campaignorganizer.characters.adapter.statblock.in.web;

import com.campaignorganizer.characters.adapter.statblock.in.web.GlobalStatblockWebDtos.GlobalStatblockRequest;
import com.campaignorganizer.characters.adapter.statblock.in.web.GlobalStatblockWebDtos.GlobalStatblockResponse;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.CreateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.UpdateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GlobalStatblockWebMapper {

    GlobalStatblockResponse toResponse(GlobalStatblockView view);

    default CreateGlobalStatblockCommand toCreateCommand(GlobalStatblockRequest request) {
        return new CreateGlobalStatblockCommand(request.systemId(), request.globalTemplateId(), request.name(),
                request.stats(), request.notes());
    }

    default UpdateGlobalStatblockCommand toUpdateCommand(UUID globalStatblockId,
                                                          GlobalStatblockRequest request) {
        return new UpdateGlobalStatblockCommand(globalStatblockId, request.systemId(),
                request.globalTemplateId(), request.name(), request.stats(), request.notes());
    }
}
