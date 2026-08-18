package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.CategoryWebDtos.CategoryRequest;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.CategoryWebDtos.CategoryResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.CreateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.UpdateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryWebMapper {

    CategoryResponse toResponse(CategoryView view);

    default CreateCategoryCommand toCreateCommand(UUID worldId, CategoryRequest request) {
        return new CreateCategoryCommand(worldId, request.parentId(), request.name());
    }

    default UpdateCategoryCommand toUpdateCommand(UUID worldId, UUID categoryId,
                                                  CategoryRequest request) {
        return new UpdateCategoryCommand(worldId, categoryId, request.parentId(), request.name());
    }
}
