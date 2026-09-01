package com.campaignorganizer.characters.application.document.port.published;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published read model for a document. */
public record DocumentView(
        UUID id,
        UUID worldId,
        UUID templateId,
        UUID campaignId,
        String name,
        Map<String, Object> values,
        Instant createdAt,
        Instant updatedAt) {
}
