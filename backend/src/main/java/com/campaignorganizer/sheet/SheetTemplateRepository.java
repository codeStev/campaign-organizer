package com.campaignorganizer.sheet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SheetTemplateRepository extends JpaRepository<SheetTemplate, UUID> {

    List<SheetTemplate> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<SheetTemplate> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
