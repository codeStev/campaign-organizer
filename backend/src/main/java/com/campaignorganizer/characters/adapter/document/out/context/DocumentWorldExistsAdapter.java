package com.campaignorganizer.characters.adapter.document.out.context;

import com.campaignorganizer.characters.application.document.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves world existence for the document module via the worldbuilding world query port. */
@Component
public class DocumentWorldExistsAdapter implements WorldExistsPort {

    private final WorldQueryPort worlds;

    public DocumentWorldExistsAdapter(WorldQueryPort worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.exists(worldId);
    }
}
