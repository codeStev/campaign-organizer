package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.CreateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;

public interface CreateMapCategoryUseCase {

    MapCategoryView create(CreateMapCategoryCommand command);
}
