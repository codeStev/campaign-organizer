package com.campaignorganizer.campaign.application.todo.port.in;

import java.util.UUID;

public final class TodoCommands {

    private TodoCommands() {
    }

    public record CreateCampaignTodoCommand(UUID worldId, UUID campaignId, String text) {
    }

    public record CreateSessionTodoCommand(UUID worldId, UUID campaignId, UUID sessionId, String text) {
    }

    public record UpdateTodoCommand(UUID worldId, UUID campaignId, UUID todoId, String text, boolean done) {
    }
}
