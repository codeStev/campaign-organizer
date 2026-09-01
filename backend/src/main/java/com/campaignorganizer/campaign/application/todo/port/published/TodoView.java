package com.campaignorganizer.campaign.application.todo.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a todo. Null {@code sessionId} means a standing campaign-level todo. */
public record TodoView(
        UUID id,
        UUID campaignId,
        UUID sessionId,
        String text,
        boolean done,
        Instant createdAt,
        Instant updatedAt) {
}
