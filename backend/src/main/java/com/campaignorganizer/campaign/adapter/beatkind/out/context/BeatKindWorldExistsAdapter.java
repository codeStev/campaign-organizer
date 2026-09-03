package com.campaignorganizer.campaign.adapter.beatkind.out.context;

import com.campaignorganizer.campaign.application.beatkind.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the beat kind module via the worldbuilding world query port. */
@Component
public class BeatKindWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public BeatKindWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
