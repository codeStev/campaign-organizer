package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import java.util.Map;
import java.util.UUID;

public interface GetLayerStylesUseCase {

    Map<String, LayerStyle> getLayerStyles(UUID worldId);
}
