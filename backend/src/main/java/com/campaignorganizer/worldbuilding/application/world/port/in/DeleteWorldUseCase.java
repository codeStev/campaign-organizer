package com.campaignorganizer.worldbuilding.application.world.port.in;

import java.util.UUID;

public interface DeleteWorldUseCase {

    void delete(UUID worldId);
}
