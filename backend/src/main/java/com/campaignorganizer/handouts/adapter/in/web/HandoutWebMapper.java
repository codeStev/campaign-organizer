package com.campaignorganizer.handouts.adapter.in.web;

import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutRequest;
import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutResponse;
import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.ReorderHandoutsRequest;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.CreateHandoutCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.ReorderHandoutsCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCommands.UpdateHandoutCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps handout web DTOs ↔ commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface HandoutWebMapper {

    HandoutResponse toResponse(HandoutView view);

    default CreateHandoutCommand toCreateCommand(UUID worldId, HandoutRequest request) {
        return new CreateHandoutCommand(worldId, request.title(), request.preset(),
                request.body(), request.sessionId());
    }

    default UpdateHandoutCommand toUpdateCommand(UUID worldId, UUID handoutId,
                                                 HandoutRequest request) {
        return new UpdateHandoutCommand(worldId, handoutId, request.title(), request.preset(),
                request.body(), request.sessionId());
    }

    default ReorderHandoutsCommand toReorderCommand(UUID worldId, ReorderHandoutsRequest request) {
        return new ReorderHandoutsCommand(worldId, request.orderedIds());
    }
}
