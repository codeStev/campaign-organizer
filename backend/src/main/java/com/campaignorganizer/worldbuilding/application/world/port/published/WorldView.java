package com.campaignorganizer.worldbuilding.application.world.port.published;

import com.campaignorganizer.worldbuilding.domain.world.LayerStyle;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Published read model for a world. */
public record WorldView(
        UUID id,
        String name,
        String description,
        Map<String, LayerStyle> layerStyles,
        Instant createdAt,
        Instant updatedAt) {
}
