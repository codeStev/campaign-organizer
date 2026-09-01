package com.campaignorganizer.campaign.adapter.player.out.context;

import com.campaignorganizer.campaign.application.player.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the player module via the worldbuilding world query port. */
@Component
public class PlayerWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public PlayerWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
