package com.campaignorganizer.worldbuilding.adapter.relationship.out.context;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the (still-legacy) world store. */
@Component
public class RelationshipWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public RelationshipWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
