package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.CreateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;

public interface CreateMapPinUseCase {

    MapPinView create(CreateMapPinCommand command);
}
