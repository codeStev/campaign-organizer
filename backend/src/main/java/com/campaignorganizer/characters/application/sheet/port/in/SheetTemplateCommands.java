package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetSection;
import java.util.List;
import java.util.UUID;

public final class SheetTemplateCommands {

    private SheetTemplateCommands() {
    }

    public record CreateSheetTemplateCommand(UUID worldId, String name, String system,
                                              List<SheetSection> sections) {
    }

    public record UpdateSheetTemplateCommand(UUID worldId, UUID templateId, String name, String system,
                                              List<SheetSection> sections) {
    }
}
