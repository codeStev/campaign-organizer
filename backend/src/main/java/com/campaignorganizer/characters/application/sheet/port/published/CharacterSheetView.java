package com.campaignorganizer.characters.application.sheet.port.published;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published read model for a character sheet. */
public record CharacterSheetView(
        UUID id,
        UUID worldId,
        UUID worldTemplateId,
        UUID globalTemplateId,
        UUID articleId,
        UUID campaignId,
        String name,
        Map<String, Object> values,
        Instant createdAt,
        Instant updatedAt) {
}
