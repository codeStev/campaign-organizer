package com.campaignorganizer.tables.adapter.rolltable.out.context;

import com.campaignorganizer.tables.application.rolltable.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the worldbuilding context's published port. */
@Component
public class RollTableWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public RollTableWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
