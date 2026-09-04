package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapCategoryJpaRepository extends JpaRepository<MapCategoryJpaEntity, UUID> {

    List<MapCategoryJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<MapCategoryJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
