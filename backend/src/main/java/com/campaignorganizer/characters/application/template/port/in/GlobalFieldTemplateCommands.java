package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import java.util.List;
import java.util.UUID;

public final class GlobalFieldTemplateCommands {

    private GlobalFieldTemplateCommands() {
    }

    public record CreateGlobalFieldTemplateCommand(String name, TemplateKind kind, UUID systemId,
                                                    List<TemplateSection> sections) {
    }

    public record UpdateGlobalFieldTemplateCommand(UUID templateId, String name, UUID systemId,
                                                    List<TemplateSection> sections) {
    }
}
