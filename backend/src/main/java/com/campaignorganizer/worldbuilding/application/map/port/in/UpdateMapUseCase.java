package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapCommands.UpdateMapCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;

public interface UpdateMapUseCase {

    MapView update(UpdateMapCommand command);
}
