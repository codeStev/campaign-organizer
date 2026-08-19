package com.campaignorganizer.characters.adapter.sheet.out.context;

import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the sheet module via the worldbuilding world query port. */
@Component
public class SheetWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public SheetWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
