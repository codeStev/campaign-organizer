package com.campaignorganizer.campaign.application.todo.port.in;

import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import java.util.List;
import java.util.UUID;

/** Lists a campaign's standing todos only (session-attached todos are excluded). */
public interface ListCampaignTodosUseCase {

    List<TodoView> list(UUID worldId, UUID campaignId);
}
