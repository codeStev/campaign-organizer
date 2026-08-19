package com.campaignorganizer.characters.adapter.sheet.in.web;

import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SheetTemplateWebDtos {

    private SheetTemplateWebDtos() {
    }

    public record SheetTemplateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 100) String system,
            List<SheetSection> sections) {
    }

    public record SheetTemplateResponse(
            UUID id,
            UUID worldId,
            String name,
            String system,
            List<SheetSection> sections,
            Instant createdAt,
            Instant updatedAt) {
    }
}
