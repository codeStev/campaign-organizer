package com.campaignorganizer.worldbuilding.adapter.calendar.out.context;

import com.campaignorganizer.world.WorldRepository;
import com.campaignorganizer.worldbuilding.application.calendar.port.out.WorldExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the (still-legacy) world store. */
@Component
public class CalendarWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public CalendarWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
