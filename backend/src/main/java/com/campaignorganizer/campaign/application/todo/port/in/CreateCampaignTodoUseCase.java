package com.campaignorganizer.campaign.application.todo.port.in;

import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateCampaignTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;

public interface CreateCampaignTodoUseCase {

    TodoView create(CreateCampaignTodoCommand command);
}
