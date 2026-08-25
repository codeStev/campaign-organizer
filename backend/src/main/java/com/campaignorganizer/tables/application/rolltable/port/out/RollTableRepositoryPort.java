package com.campaignorganizer.tables.application.rolltable.port.out;

import com.campaignorganizer.tables.domain.rolltable.RollTable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RollTableRepositoryPort {

    List<RollTable> findByWorld(UUID worldId);

    Optional<RollTable> findByIdAndWorld(UUID tableId, UUID worldId);

    Optional<RollTable> findById(UUID tableId);

    boolean existsInWorld(UUID tableId, UUID worldId);

    RollTable save(RollTable table);

    void delete(RollTable table);
}
