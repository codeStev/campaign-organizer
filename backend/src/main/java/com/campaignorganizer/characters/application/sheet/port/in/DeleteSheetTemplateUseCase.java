package com.campaignorganizer.characters.application.sheet.port.in;

import java.util.UUID;

public interface DeleteSheetTemplateUseCase {

    void delete(UUID worldId, UUID templateId);
}
