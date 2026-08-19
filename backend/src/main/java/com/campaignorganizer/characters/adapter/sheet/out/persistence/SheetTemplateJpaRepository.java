package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SheetTemplateJpaRepository extends JpaRepository<SheetTemplateJpaEntity, UUID> {

    List<SheetTemplateJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<SheetTemplateJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
