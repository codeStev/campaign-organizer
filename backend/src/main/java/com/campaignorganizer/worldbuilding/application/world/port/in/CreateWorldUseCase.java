package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.application.world.port.in.WorldCommands.CreateWorldCommand;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;

public interface CreateWorldUseCase {

    WorldView create(CreateWorldCommand command);
}
