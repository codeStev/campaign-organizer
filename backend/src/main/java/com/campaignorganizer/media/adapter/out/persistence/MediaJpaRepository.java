package com.campaignorganizer.media.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaJpaRepository extends JpaRepository<MediaJpaEntity, UUID> {

    List<MediaJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<MediaJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
