package com.campaignorganizer.campaign.adapter.campaign.out.context;

import com.campaignorganizer.campaign.application.campaign.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the campaign module via the worldbuilding world query port. */
@Component
public class CampaignWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public CampaignWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
