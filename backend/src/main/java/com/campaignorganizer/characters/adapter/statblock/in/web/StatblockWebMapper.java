package com.campaignorganizer.characters.adapter.statblock.in.web;

import com.campaignorganizer.characters.adapter.statblock.in.web.StatblockWebDtos.StatblockRequest;
import com.campaignorganizer.characters.adapter.statblock.in.web.StatblockWebDtos.StatblockResponse;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.UpdateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StatblockWebMapper {

    StatblockResponse toResponse(StatblockView view);

    default CreateStatblockCommand toCreateCommand(UUID worldId, StatblockRequest request) {
        return new CreateStatblockCommand(worldId, request.articleId(), request.campaignId(),
                request.templateId(), request.name(), request.stats(), request.notes());
    }

    default UpdateStatblockCommand toUpdateCommand(UUID worldId, UUID statblockId,
                                                   StatblockRequest request) {
        return new UpdateStatblockCommand(worldId, statblockId, request.articleId(),
                request.campaignId(), request.templateId(), request.name(), request.stats(),
                request.notes());
    }
}
