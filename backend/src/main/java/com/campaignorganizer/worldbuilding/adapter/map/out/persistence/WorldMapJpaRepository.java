package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldMapJpaRepository extends JpaRepository<WorldMapJpaEntity, UUID> {

    List<WorldMapJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<WorldMapJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
