package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import java.util.List;
import java.util.UUID;

public interface ListMapPinsUseCase {

    List<MapPinView> list(UUID worldId, UUID mapId);
}
