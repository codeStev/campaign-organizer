package com.campaignorganizer.tables.application.rolltable.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Published port for other contexts (beat validation, session packet, usage,
 * export). Exposes roll-table reads without leaking domain/persistence.
 */
public interface RollTableQueryPort {

    boolean existsInWorld(UUID tableId, UUID worldId);

    Optional<RollTableView> findByIdInWorld(UUID tableId, UUID worldId);

    Optional<RollTableView> findById(UUID tableId);

    List<RollTableView> findByWorld(UUID worldId);
}
