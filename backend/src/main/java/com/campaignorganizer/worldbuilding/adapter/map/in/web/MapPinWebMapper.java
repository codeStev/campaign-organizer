package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapPinWebDtos.MapPinRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapPinWebDtos.MapPinResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.CreateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.UpdateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapPinWebMapper {

    MapPinResponse toResponse(MapPinView view);

    default CreateMapPinCommand toCreateCommand(UUID worldId, UUID mapId, MapPinRequest request) {
        return new CreateMapPinCommand(worldId, mapId, request.articleId(), request.label(),
                request.layer(), request.x(), request.y());
    }

    default UpdateMapPinCommand toUpdateCommand(UUID worldId, UUID mapId, UUID pinId,
                                                MapPinRequest request) {
        return new UpdateMapPinCommand(worldId, mapId, pinId, request.articleId(), request.label(),
                request.layer(), request.x(), request.y());
    }
}
