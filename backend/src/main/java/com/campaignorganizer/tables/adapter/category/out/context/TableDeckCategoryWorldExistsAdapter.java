package com.campaignorganizer.tables.adapter.category.out.context;

import com.campaignorganizer.tables.application.category.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** World-existence check against the worldbuilding context's published port. */
@Component
public class TableDeckCategoryWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public TableDeckCategoryWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
