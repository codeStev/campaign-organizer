package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.UpdateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;

public interface UpdateWorldUseCase {

    WorldView update(UpdateWorldCommand command);
}
