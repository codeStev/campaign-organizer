package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;
import java.util.List;
import java.util.UUID;

public interface ListSheetTemplatesUseCase {

    List<SheetTemplateView> list(UUID worldId);
}
