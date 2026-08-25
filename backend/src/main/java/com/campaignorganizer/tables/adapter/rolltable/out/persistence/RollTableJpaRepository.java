package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RollTableJpaRepository extends JpaRepository<RollTableJpaEntity, UUID> {

    List<RollTableJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<RollTableJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
