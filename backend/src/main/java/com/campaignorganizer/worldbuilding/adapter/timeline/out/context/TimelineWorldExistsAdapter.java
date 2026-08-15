package com.campaignorganizer.worldbuilding.adapter.timeline.out.context;

import com.campaignorganizer.world.WorldRepository;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelineWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public TimelineWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
