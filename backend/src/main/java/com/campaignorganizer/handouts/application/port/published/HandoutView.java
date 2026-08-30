package com.campaignorganizer.handouts.application.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model of a handout. */
public record HandoutView(UUID id, UUID worldId, String title, String preset, String body,
                          UUID sessionId, Instant createdAt, Instant updatedAt) {
}
