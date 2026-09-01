package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class GlobalFieldTemplateWebDtos {

    private GlobalFieldTemplateWebDtos() {
    }

    public record GlobalFieldTemplateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull TemplateKind kind,
            @NotBlank @Size(max = 100) String system,
            List<TemplateSection> sections) {
    }

    public record GlobalFieldTemplateResponse(
            UUID id,
            String name,
            TemplateKind kind,
            String system,
            List<TemplateSection> sections,
            Instant createdAt,
            Instant updatedAt) {
    }
}
