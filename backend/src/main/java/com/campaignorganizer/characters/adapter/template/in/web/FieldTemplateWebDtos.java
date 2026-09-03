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
            UUID categoryId,
            @NotBlank @Size(max = 200) String name,
            @NotNull TemplateKind kind,
            UUID systemId,
            List<TemplateSection> sections) {
    }

    public record FieldTemplateResponse(
            UUID id,
            UUID worldId,
            UUID categoryId,
            String name,
            TemplateKind kind,
            UUID systemId,
            List<TemplateSection> sections,
            Instant createdAt,
            Instant updatedAt) {
    }
}
