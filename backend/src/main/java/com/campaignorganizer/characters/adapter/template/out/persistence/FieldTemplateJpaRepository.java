package com.campaignorganizer.characters.adapter.template.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldTemplateJpaRepository extends JpaRepository<FieldTemplateJpaEntity, UUID> {

    List<FieldTemplateJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<FieldTemplateJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
