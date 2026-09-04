package com.campaignorganizer.tables.adapter.category.in.web;

import com.campaignorganizer.tables.adapter.category.in.web.TableDeckCategoryWebDtos.TableDeckCategoryRequest;
import com.campaignorganizer.tables.adapter.category.in.web.TableDeckCategoryWebDtos.TableDeckCategoryResponse;
import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.CreateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.in.TableDeckCategoryCommands.UpdateTableDeckCategoryCommand;
import com.campaignorganizer.tables.application.category.port.published.TableDeckCategoryView;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TableDeckCategoryWebMapper {

    TableDeckCategoryResponse toResponse(TableDeckCategoryView view);

    default CreateTableDeckCategoryCommand toCreateCommand(UUID worldId, TableDeckCategoryRequest request) {
        return new CreateTableDeckCategoryCommand(worldId, request.parentId(), request.name());
    }

    default UpdateTableDeckCategoryCommand toUpdateCommand(UUID worldId, UUID categoryId,
                                                  TableDeckCategoryRequest request) {
        return new UpdateTableDeckCategoryCommand(worldId, categoryId, request.parentId(), request.name());
    }
}
