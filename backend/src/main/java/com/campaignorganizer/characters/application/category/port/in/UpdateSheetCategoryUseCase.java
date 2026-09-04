package com.campaignorganizer.characters.application.category.port.in;

import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.UpdateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;

public interface UpdateSheetCategoryUseCase {

    SheetCategoryView update(UpdateSheetCategoryCommand command);
}
