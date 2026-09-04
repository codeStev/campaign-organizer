package com.campaignorganizer.worldbuilding.adapter.map.in.web;

import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapCategoryWebDtos.MapCategoryRequest;
import com.campaignorganizer.worldbuilding.adapter.map.in.web.MapCategoryWebDtos.MapCategoryResponse;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.CreateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.UpdateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MapCategoryWebMapper {

    MapCategoryResponse toResponse(MapCategoryView view);

    default CreateMapCategoryCommand toCreateCommand(UUID worldId, MapCategoryRequest request) {
        return new CreateMapCategoryCommand(worldId, request.parentId(), request.name());
    }

    default UpdateMapCategoryCommand toUpdateCommand(UUID worldId, UUID categoryId,
                                                  MapCategoryRequest request) {
        return new UpdateMapCategoryCommand(worldId, categoryId, request.parentId(), request.name());
    }
}
