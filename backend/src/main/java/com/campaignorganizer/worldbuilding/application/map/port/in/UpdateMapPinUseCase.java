package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.in.MapPinCommands.UpdateMapPinCommand;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;

public interface UpdateMapPinUseCase {

    MapPinView update(UpdateMapPinCommand command);
}
