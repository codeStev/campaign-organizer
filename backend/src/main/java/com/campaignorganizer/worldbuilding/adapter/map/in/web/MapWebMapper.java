package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapWebDtos.MapRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapWebDtos.MapResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.CreateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.UpdateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MapWebMapper {

    @Mapping(target = "imageUrl", expression = "java(imageUrl(view.mediaId()))")
    MapResponse toResponse(MapView view);

    default CreateMapCommand toCreateCommand(UUID worldId, MapRequest request) {
        return new CreateMapCommand(worldId, request.name(), request.mediaId());
    }

    default UpdateMapCommand toUpdateCommand(UUID worldId, UUID mapId, MapRequest request) {
        return new UpdateMapCommand(worldId, mapId, request.name(), request.mediaId());
    }

    default String imageUrl(UUID mediaId) {
        return mediaId == null ? null : "/api/media/" + mediaId + "/content";
    }
}
