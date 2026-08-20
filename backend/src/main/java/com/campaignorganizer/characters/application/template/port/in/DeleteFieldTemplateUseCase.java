package com.campaignorganizer.characters.application.template.port.in;

import java.util.UUID;

public interface DeleteFieldTemplateUseCase {

    void delete(UUID worldId, UUID templateId);
}
