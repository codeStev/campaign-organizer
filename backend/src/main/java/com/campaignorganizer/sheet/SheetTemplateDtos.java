package com.campaignorganizer.sheet;

import com.campaignorganizer.sheet.SheetSchema.SheetSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SheetTemplateDtos {

    private SheetTemplateDtos() {
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

        public static SheetTemplateResponse from(SheetTemplate t) {
            return new SheetTemplateResponse(t.getId(), t.getWorldId(), t.getName(), t.getSystem(),
                    t.getSections(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
