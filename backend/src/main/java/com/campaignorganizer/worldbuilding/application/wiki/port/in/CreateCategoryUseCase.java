package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.CreateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;

public interface CreateCategoryUseCase {

    CategoryView create(CreateCategoryCommand command);
}
