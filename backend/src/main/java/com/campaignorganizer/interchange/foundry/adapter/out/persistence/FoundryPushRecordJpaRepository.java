package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FoundryPushRecordJpaRepository extends JpaRepository<FoundryPushRecordJpaEntity, UUID> {

    Optional<FoundryPushRecordJpaEntity> findByWorldIdAndEntityTypeAndEntityId(UUID worldId, String entityType,
                                                                               UUID entityId);
}
