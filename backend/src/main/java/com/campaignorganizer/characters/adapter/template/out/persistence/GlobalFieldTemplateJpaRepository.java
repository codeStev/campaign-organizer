package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalFieldTemplateJpaRepository extends JpaRepository<GlobalFieldTemplateJpaEntity, UUID> {

    List<GlobalFieldTemplateJpaEntity> findByKindOrderByCreatedAtDesc(TemplateKind kind);

    List<GlobalFieldTemplateJpaEntity> findAllByOrderByCreatedAtDesc();

    Optional<GlobalFieldTemplateJpaEntity> findByKindAndSystemAndName(TemplateKind kind, String system,
                                                                      String name);
}
