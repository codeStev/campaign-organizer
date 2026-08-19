package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import java.util.UUID;

public interface GetSheetTemplateUseCase {

    SheetTemplateView get(UUID worldId, UUID templateId);
}
