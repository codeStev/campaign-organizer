package com.campaignorganizer.characters.adapter.category.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SheetCategoryJpaRepository extends JpaRepository<SheetCategoryJpaEntity, UUID> {

    List<SheetCategoryJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<SheetCategoryJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
