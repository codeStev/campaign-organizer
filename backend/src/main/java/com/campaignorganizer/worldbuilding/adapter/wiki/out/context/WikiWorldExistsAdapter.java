package com.campaignorganizer.worldbuilding.adapter.wiki.out.context;

import com.campaignorganizer.world.WorldRepository;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the wiki module via the legacy world repository. */
@Component
public class WikiWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public WikiWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
