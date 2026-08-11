package com.campaignorganizer.map;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldMapRepository extends JpaRepository<WorldMap, UUID> {

    List<WorldMap> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<WorldMap> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
