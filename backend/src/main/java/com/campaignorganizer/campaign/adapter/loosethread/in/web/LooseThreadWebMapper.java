package com.campaignorganizer.campaign.adapter.loosethread.in.web;

import com.campaignorganizer.campaign.adapter.loosethread.in.web.LooseThreadWebDtos.LooseThreadRequest;
import com.campaignorganizer.campaign.adapter.loosethread.in.web.LooseThreadWebDtos.LooseThreadResponse;
import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.CreateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.in.LooseThreadCommands.UpdateLooseThreadCommand;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps loose-thread web DTOs to/from commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface LooseThreadWebMapper {

    LooseThreadResponse toResponse(LooseThreadView view);

    default CreateLooseThreadCommand toCreateCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                                      LooseThreadRequest request) {
        return new CreateLooseThreadCommand(worldId, campaignId, sessionId, request.text(),
                request.status());
    }

    default UpdateLooseThreadCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                                      UUID threadId, LooseThreadRequest request) {
        return new UpdateLooseThreadCommand(worldId, campaignId, sessionId, threadId, request.text(),
                request.status());
    }
}
