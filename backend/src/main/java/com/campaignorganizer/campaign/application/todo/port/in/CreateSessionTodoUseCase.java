package com.campaignorganizer.campaign.application.todo.port.in;

import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateSessionTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;

public interface CreateSessionTodoUseCase {

    TodoView create(CreateSessionTodoCommand command);
}
