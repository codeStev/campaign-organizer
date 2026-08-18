package com.campaignorganizer.media.adapter.out.context;

import com.campaignorganizer.media.application.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * World-existence check against the world aggregate's store (intra-migration seam;
 * becomes a published-port call once `world` is modelled — ADR-0050).
 */
@Component
public class MediaWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public MediaWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
