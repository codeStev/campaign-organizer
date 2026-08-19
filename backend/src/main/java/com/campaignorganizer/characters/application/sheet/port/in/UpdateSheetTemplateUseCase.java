package com.campaignorganizer.characters.application.sheet.port.in;

import com.campaignorganizer.characters.application.sheet.port.in.SheetTemplateCommands.UpdateSheetTemplateCommand;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateView;

public interface UpdateSheetTemplateUseCase {

    SheetTemplateView update(UpdateSheetTemplateCommand command);
}
