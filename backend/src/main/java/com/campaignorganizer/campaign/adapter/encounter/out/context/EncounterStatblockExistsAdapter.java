package com.campaignorganizer.campaign.adapter.encounter.out.context;

import com.campaignorganizer.campaign.application.encounter.port.out.StatblockExistsPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves statblock existence for the encounter module via the statblock query port. */
@Component
public class EncounterStatblockExistsAdapter implements StatblockExistsPort {

    private final StatblockQueryPort statblocks;

    public EncounterStatblockExistsAdapter(StatblockQueryPort statblocks) {
        this.statblocks = statblocks;
    }

    @Override
    public boolean existsInWorld(UUID statblockId, UUID worldId) {
        return statblocks.existsInWorld(statblockId, worldId);
    }
}
