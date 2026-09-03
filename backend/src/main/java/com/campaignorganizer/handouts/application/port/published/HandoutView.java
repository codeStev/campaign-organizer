package com.campaignorganizer.handouts.application.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model of a handout. */
public record HandoutView(UUID id, UUID worldId, UUID categoryId, String title, String preset,
                          String body, UUID sessionId, Integer sortOrder, boolean revealed,
                          Instant createdAt, Instant updatedAt) {
}
