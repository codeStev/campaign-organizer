package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.util.UUID;

public interface GetWorldUseCase {

    WorldView get(UUID worldId);
}
