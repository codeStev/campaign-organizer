package com.campaignorganizer.interchange.foundry.application.port.out;

import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import java.util.Optional;
import java.util.UUID;

public interface FoundryConnectionRepositoryPort {

    Optional<FoundryConnection> findByWorldId(UUID worldId);

    FoundryConnection save(FoundryConnection connection);
}
