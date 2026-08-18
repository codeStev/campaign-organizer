package com.campaignorganizer.worldbuilding.application.world.port.in;

import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import java.util.Map;
import java.util.UUID;

public interface ReplaceLayerStylesUseCase {

    Map<String, LayerStyle> replace(UUID worldId, Map<String, LayerStyle> layerStyles);
}
