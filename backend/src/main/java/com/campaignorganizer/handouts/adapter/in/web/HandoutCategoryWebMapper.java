package com.campaignorganizer.handouts.adapter.in.web;

import com.campaignorganizer.handouts.adapter.in.web.HandoutCategoryWebDtos.HandoutCategoryRequest;
import com.campaignorganizer.handouts.adapter.in.web.HandoutCategoryWebDtos.HandoutCategoryResponse;
import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.CreateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.in.HandoutCategoryCommands.UpdateHandoutCategoryCommand;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HandoutCategoryWebMapper {

    HandoutCategoryResponse toResponse(HandoutCategoryView view);

    default CreateHandoutCategoryCommand toCreateCommand(UUID worldId, HandoutCategoryRequest request) {
        return new CreateHandoutCategoryCommand(worldId, request.parentId(), request.name());
    }

    default UpdateHandoutCategoryCommand toUpdateCommand(UUID worldId, UUID categoryId,
                                                  HandoutCategoryRequest request) {
        return new UpdateHandoutCategoryCommand(worldId, categoryId, request.parentId(), request.name());
    }
}
