package com.campaignorganizer.campaign.application.todo.port.in;

import java.util.UUID;

public interface DeleteTodoUseCase {

    void delete(UUID worldId, UUID campaignId, UUID todoId);
}
