package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionWebDtos.SessionRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionWebDtos.SessionResponse;
import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.CreateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.in.SessionCommands.UpdateSessionCommand;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionWebMapper {

    SessionResponse toResponse(SessionView view);

    default CreateSessionCommand toCreateCommand(UUID worldId, UUID campaignId, SessionRequest request) {
        return new CreateSessionCommand(worldId, campaignId, request.title(), request.sessionNumber(),
                request.date(), request.summary(), request.notes());
    }

    default UpdateSessionCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                                 SessionRequest request) {
        return new UpdateSessionCommand(worldId, campaignId, sessionId, request.title(),
                request.sessionNumber(), request.date(), request.summary(), request.notes());
    }
}
