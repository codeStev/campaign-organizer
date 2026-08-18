package com.campaignorganizer.campaign.adapter.arc.in.web;

import com.campaignorganizer.campaign.adapter.arc.in.web.ArcWebDtos.ArcRequest;
import com.campaignorganizer.campaign.adapter.arc.in.web.ArcWebDtos.ArcResponse;
import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.CreateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.in.ArcCommands.UpdateArcCommand;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArcWebMapper {

    ArcResponse toResponse(ArcView view);

    default CreateArcCommand toCreateCommand(UUID worldId, UUID campaignId, ArcRequest request) {
        return new CreateArcCommand(worldId, campaignId, request.title(), request.description(),
                request.status(), request.position());
    }

    default UpdateArcCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID arcId,
                                             ArcRequest request) {
        return new UpdateArcCommand(worldId, campaignId, arcId, request.title(), request.description(),
                request.status(), request.position());
    }
}
