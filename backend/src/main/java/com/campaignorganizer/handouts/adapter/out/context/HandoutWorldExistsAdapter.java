package com.campaignorganizer.handouts.adapter.out.context;

import com.campaignorganizer.handouts.application.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the worldbuilding context's published port. */
@Component
public class HandoutWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public HandoutWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
