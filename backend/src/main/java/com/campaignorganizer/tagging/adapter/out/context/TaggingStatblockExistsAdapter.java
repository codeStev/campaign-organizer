package com.campaignorganizer.tagging.adapter.out.context;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.tagging.application.port.out.StatblockExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Statblock-existence check against the characters context's published port. */
@Component
public class TaggingStatblockExistsAdapter implements StatblockExistsPort {

    private final StatblockQueryPort statblocks;

    public TaggingStatblockExistsAdapter(StatblockQueryPort statblocks) {
        this.statblocks = statblocks;
    }

    @Override
    public boolean existsInWorld(UUID statblockId, UUID worldId) {
        return statblocks.existsInWorld(statblockId, worldId);
    }
}
