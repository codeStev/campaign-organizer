package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public interface DeleteMapUseCase {

    void delete(UUID worldId, UUID mapId);
}
