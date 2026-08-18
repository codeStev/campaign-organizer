package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.characters.application.statblock.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the statblock module via the legacy world repository. */
@Component
public class StatblockWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public StatblockWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
