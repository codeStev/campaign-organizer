package com.campaignorganizer.handouts.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoutJpaRepository extends JpaRepository<HandoutJpaEntity, UUID> {

    List<HandoutJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<HandoutJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
