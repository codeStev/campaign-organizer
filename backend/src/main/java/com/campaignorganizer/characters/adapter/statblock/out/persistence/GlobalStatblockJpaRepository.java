package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalStatblockJpaRepository extends JpaRepository<GlobalStatblockJpaEntity, UUID> {

    List<GlobalStatblockJpaEntity> findAllByOrderByCreatedAtDesc();

    List<GlobalStatblockJpaEntity> findBySystemIdOrderByCreatedAtDesc(UUID systemId);

    Optional<GlobalStatblockJpaEntity> findBySystemIdAndName(UUID systemId, String name);

    boolean existsByGlobalTemplateId(UUID globalTemplateId);
}
