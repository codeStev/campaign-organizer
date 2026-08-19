package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.CreateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;

public interface CreateSheetTemplateUseCase {

    SheetTemplateView create(CreateSheetTemplateCommand command);
}
