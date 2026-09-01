package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import java.util.UUID;

public interface DuplicateFieldTemplateUseCase {

    FieldTemplateView duplicate(UUID worldId, UUID templateId);
}
