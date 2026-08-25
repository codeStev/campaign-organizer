package com.campaignorganizer.campaign.adapter.arc.out.context;

import com.campaignorganizer.campaign.application.arc.port.out.TableExistsPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves roll-table existence for the beat module via the tables query port. */
@Component
public class BeatTableExistsAdapter implements TableExistsPort {

    private final RollTableQueryPort tables;

    public BeatTableExistsAdapter(RollTableQueryPort tables) {
        this.tables = tables;
    }

    @Override
    public boolean existsInWorld(UUID tableId, UUID worldId) {
        return tables.existsInWorld(tableId, worldId);
    }
}
