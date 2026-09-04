package com.campaignorganizer.tables.adapter.category.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableDeckCategoryJpaRepository extends JpaRepository<TableDeckCategoryJpaEntity, UUID> {

    List<TableDeckCategoryJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<TableDeckCategoryJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
