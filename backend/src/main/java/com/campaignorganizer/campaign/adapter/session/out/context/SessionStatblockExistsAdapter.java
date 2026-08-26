package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.campaign.application.session.port.out.StatblockExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves statblock existence for the session module (FR-37). */
@Component
public class SessionStatblockExistsAdapter implements StatblockExistsPort {

    private final StatblockQueryPort statblocks;

    public SessionStatblockExistsAdapter(StatblockQueryPort statblocks) {
        this.statblocks = statblocks;
    }

    @Override
    public boolean existsInWorld(UUID statblockId, UUID worldId) {
        return statblocks.existsInWorld(statblockId, worldId);
    }
}
