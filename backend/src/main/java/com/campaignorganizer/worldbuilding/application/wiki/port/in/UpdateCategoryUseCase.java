package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.UpdateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;

public interface UpdateCategoryUseCase {

    CategoryView update(UpdateCategoryCommand command);
}
