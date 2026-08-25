package com.campaignorganizer.tables.adapter.carddeck.out.context;

import com.campaignorganizer.tables.application.carddeck.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the worldbuilding context's published port. */
@Component
public class CardDeckWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public CardDeckWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
