package com.campaignorganizer.campaign.application.todo.port.in;

import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import java.util.List;
import java.util.UUID;

public interface ListSessionTodosUseCase {

    List<TodoView> list(UUID worldId, UUID campaignId, UUID sessionId);
}
