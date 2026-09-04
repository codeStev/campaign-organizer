package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import java.util.List;
import java.util.UUID;

public final class FieldTemplateCommands {

    private FieldTemplateCommands() {
    }

    public record CreateFieldTemplateCommand(UUID worldId, UUID categoryId, String name, TemplateKind kind,
                                              UUID systemId, List<TemplateSection> sections) {
    }

    public record UpdateFieldTemplateCommand(UUID worldId, UUID templateId, UUID categoryId, String name,
                                              UUID systemId, List<TemplateSection> sections) {
    }
}
