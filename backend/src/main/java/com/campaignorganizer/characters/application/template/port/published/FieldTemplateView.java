package com.campaignorganizer.characters.application.template.port.published;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a field template. */
public record FieldTemplateView(
        UUID id,
        UUID worldId,
        String name,
        TemplateKind kind,
        UUID systemId,
        List<TemplateSection> sections,
        Instant createdAt,
        Instant updatedAt) {
}
