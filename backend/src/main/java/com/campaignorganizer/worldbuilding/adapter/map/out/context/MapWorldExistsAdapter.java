package com.campaignorganizer.worldbuilding.adapter.map.out.context;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the map module via the legacy world repository. */
@Component
public class MapWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public MapWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
