package com.campaignorganizer.worldbuilding.adapter.relationship.out.context;

import com.campaignorganizer.world.WorldRepository;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the (still-legacy) world store. */
@Component
public class RelationshipWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public RelationshipWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
