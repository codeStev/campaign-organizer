package com.campaignorganizer.campaign.adapter.todo.in.web;

import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoRequest;
import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoResponse;
import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoUpdateRequest;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateCampaignTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateSessionTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.UpdateTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps todo web DTOs to/from commands/views (MapStruct). */
@Mapper(componentModel = "spring")
public interface TodoWebMapper {

    TodoResponse toResponse(TodoView view);

    default CreateCampaignTodoCommand toCreateCampaignCommand(UUID worldId, UUID campaignId,
                                                               TodoRequest request) {
        return new CreateCampaignTodoCommand(worldId, campaignId, request.text());
    }

    default CreateSessionTodoCommand toCreateSessionCommand(UUID worldId, UUID campaignId, UUID sessionId,
                                                             TodoRequest request) {
        return new CreateSessionTodoCommand(worldId, campaignId, sessionId, request.text());
    }

    default UpdateTodoCommand toUpdateCommand(UUID worldId, UUID campaignId, UUID todoId,
                                              TodoUpdateRequest request) {
        return new UpdateTodoCommand(worldId, campaignId, todoId, request.text(), request.done());
    }
}
