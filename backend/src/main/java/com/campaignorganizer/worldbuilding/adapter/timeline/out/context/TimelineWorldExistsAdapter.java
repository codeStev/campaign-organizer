package com.campaignorganizer.worldbuilding.adapter.timeline.out.context;

import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelineWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public TimelineWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
