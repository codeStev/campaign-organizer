package com.campaignorganizer.handouts.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoutCategoryJpaRepository extends JpaRepository<HandoutCategoryJpaEntity, UUID> {

    List<HandoutCategoryJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<HandoutCategoryJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
