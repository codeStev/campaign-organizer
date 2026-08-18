package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    List<CategoryJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<CategoryJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
