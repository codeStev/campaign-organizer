package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.characters.application.statblock.port.out.WorldExistsPort;
import com.campaignorganizer.world.WorldRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the statblock module via the legacy world repository. */
@Component
public class StatblockWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public StatblockWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
