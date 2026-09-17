package com.campaignorganizer.interchange.foundry.application.port.out;

import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import java.util.Optional;
import java.util.UUID;

public interface FoundryPushRecordRepositoryPort {

    Optional<FoundryPushRecord> findByEntity(UUID worldId, FoundryEntityType entityType, UUID entityId);

    FoundryPushRecord save(FoundryPushRecord record);
}
