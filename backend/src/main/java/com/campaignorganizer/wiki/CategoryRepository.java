package com.campaignorganizer.wiki;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<Category> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
