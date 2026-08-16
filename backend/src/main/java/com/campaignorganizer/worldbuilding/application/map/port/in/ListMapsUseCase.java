package com.campaignorganizer.worldbuilding.application.map.port.in;

import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import java.util.List;
import java.util.UUID;

public interface ListMapsUseCase {

    List<MapView> list(UUID worldId);
}
