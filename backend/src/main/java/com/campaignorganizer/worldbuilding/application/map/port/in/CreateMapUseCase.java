package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.CreateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;

public interface CreateMapUseCase {

    MapView create(CreateMapCommand command);
}
