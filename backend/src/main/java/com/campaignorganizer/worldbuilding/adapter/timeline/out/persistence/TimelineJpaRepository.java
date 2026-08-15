package com.campaignorganizer.worldbuilding.adapter.timeline.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineJpaRepository extends JpaRepository<TimelineJpaEntity, UUID> {

    List<TimelineJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<TimelineJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
