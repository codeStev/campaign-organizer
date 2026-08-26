package com.campaignorganizer.campaign.adapter.session.out.context;

import com.campaignorganizer.campaign.application.session.port.out.TableEntryExistsPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves roll-table row existence for the session module (FR-37). */
@Component
public class SessionTableEntryExistsAdapter implements TableEntryExistsPort {

    private final RollTableQueryPort tables;

    public SessionTableEntryExistsAdapter(RollTableQueryPort tables) {
        this.tables = tables;
    }

    @Override
    public boolean entryExistsInWorld(UUID tableId, UUID entryId, UUID worldId) {
        return tables.findByIdInWorld(tableId, worldId)
                .filter(t -> t.entries().stream().anyMatch(e -> e.id().equals(entryId)))
                .isPresent();
    }
}
