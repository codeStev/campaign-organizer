package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public interface DeleteMapPinUseCase {

    void delete(UUID worldId, UUID mapId, UUID pinId);
}
