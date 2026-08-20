package com.campaignorganizer.characters.application.template.port.in;

import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import java.util.UUID;

public interface GetFieldTemplateUseCase {

    FieldTemplateView get(UUID worldId, UUID templateId);
}
