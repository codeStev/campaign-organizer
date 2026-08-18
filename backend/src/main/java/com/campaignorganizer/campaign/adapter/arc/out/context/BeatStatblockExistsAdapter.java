package com.campaignorganizer.campaign.adapter.arc.out.context;

import com.campaignorganizer.campaign.application.arc.port.out.StatblockExistsPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves statblock existence for the beat module via the statblock query port. */
@Component
public class BeatStatblockExistsAdapter implements StatblockExistsPort {

    private final StatblockQueryPort statblocks;

    public BeatStatblockExistsAdapter(StatblockQueryPort statblocks) {
        this.statblocks = statblocks;
    }

    @Override
    public boolean existsInWorld(UUID statblockId, UUID worldId) {
        return statblocks.existsInWorld(statblockId, worldId);
    }
}
