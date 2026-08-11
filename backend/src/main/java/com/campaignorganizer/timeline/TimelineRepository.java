package com.campaignorganizer.timeline;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineRepository extends JpaRepository<Timeline, UUID> {

    List<Timeline> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<Timeline> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
