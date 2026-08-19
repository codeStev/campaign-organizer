package com.campaignorganizer.characters.application.sheet.port.published;

import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetSection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a sheet template. */
public record SheetTemplateView(
        UUID id,
        UUID worldId,
        String name,
        String system,
        List<SheetSection> sections,
        Instant createdAt,
        Instant updatedAt) {
}
