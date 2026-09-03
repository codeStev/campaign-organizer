package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapCategoryCommands.UpdateMapCategoryCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryView;

public interface UpdateMapCategoryUseCase {

    MapCategoryView update(UpdateMapCategoryCommand command);
}
