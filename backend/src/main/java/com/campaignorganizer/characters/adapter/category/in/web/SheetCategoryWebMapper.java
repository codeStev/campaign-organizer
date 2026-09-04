package com.campaignorganizer.characters.adapter.category.in.web;

import com.campaignorganizer.characters.adapter.category.in.web.SheetCategoryWebDtos.SheetCategoryRequest;
import com.campaignorganizer.characters.adapter.category.in.web.SheetCategoryWebDtos.SheetCategoryResponse;
import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.CreateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.UpdateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SheetCategoryWebMapper {

    SheetCategoryResponse toResponse(SheetCategoryView view);

    default CreateSheetCategoryCommand toCreateCommand(UUID worldId, SheetCategoryRequest request) {
        return new CreateSheetCategoryCommand(worldId, request.parentId(), request.name());
    }

    default UpdateSheetCategoryCommand toUpdateCommand(UUID worldId, UUID categoryId,
                                                  SheetCategoryRequest request) {
        return new UpdateSheetCategoryCommand(worldId, categoryId, request.parentId(), request.name());
    }
}
