package com.campaignorganizer.whiteboard.adapter.out.persistence;

import com.campaignorganizer.whiteboard.application.port.out.WorldExistsPort;
import com.campaignorganizer.world.WorldRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Implements the world-existence port against the world aggregate's store.
 * (Intra-context during migration; becomes a published-port call once `world` is
 * modelled inside this context — ADR-0050.)
 */
@Component
public class WhiteboardWorldExistsAdapter implements WorldExistsPort {

    private final WorldRepository worlds;

    public WhiteboardWorldExistsAdapter(WorldRepository worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean exists(UUID worldId) {
        return worlds.existsById(worldId);
    }
}
