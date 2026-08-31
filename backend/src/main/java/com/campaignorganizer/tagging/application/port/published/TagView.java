package com.campaignorganizer.tagging.application.port.published;

import com.campaignorganizer.tagging.domain.EntityType;
import java.time.Instant;
import java.util.UUID;

/** Published read model of one entity tag. */
public record TagView(UUID id, UUID worldId, EntityType entityType, UUID entityId, String name,
                      Instant createdAt) {
}
