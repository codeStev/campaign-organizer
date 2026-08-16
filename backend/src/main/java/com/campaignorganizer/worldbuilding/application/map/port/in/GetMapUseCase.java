package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import java.util.UUID;

public interface GetMapUseCase {

    MapView get(UUID worldId, UUID mapId);
}
