package com.campaignorganizer.characters.application.category.port.in;

import com.campaignorganizer.characters.application.category.port.in.SheetCategoryCommands.CreateSheetCategoryCommand;
import com.campaignorganizer.characters.application.category.port.published.SheetCategoryView;

public interface CreateSheetCategoryUseCase {

    SheetCategoryView create(CreateSheetCategoryCommand command);
}
