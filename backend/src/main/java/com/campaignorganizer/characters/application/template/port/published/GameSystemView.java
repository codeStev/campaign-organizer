package com.campaignorganizer.characters.application.template.port.published;

import java.time.Instant;
import java.util.UUID;

/** Published read model for a game system. */
public record GameSystemView(UUID id, String name, Instant createdAt, Instant updatedAt) {
}
