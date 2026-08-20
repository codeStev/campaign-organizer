package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FieldTemplateWebDtos {

    private FieldTemplateWebDtos() {
    }

    public record FieldTemplateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull TemplateKind kind,
            @Size(max = 100) String system,
            List<TemplateSection> sections) {
    }

    public record FieldTemplateResponse(
            UUID id,
            UUID worldId,
            String name,
            TemplateKind kind,
            String system,
            List<TemplateSection> sections,
            Instant createdAt,
            Instant updatedAt) {
    }
}
