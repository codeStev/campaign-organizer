package com.campaignorganizer.campaign.application.todo.port.in;

import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.UpdateTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;

public interface UpdateTodoUseCase {

    TodoView update(UpdateTodoCommand command);
}
