package com.campaignorganizer.characters.application.template.port.in;

import java.util.UUID;

public interface DeleteGlobalFieldTemplateUseCase {

    void delete(UUID templateId);
}
