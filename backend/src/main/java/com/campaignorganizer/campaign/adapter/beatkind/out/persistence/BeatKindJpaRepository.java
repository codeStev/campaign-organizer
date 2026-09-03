package com.campaignorganizer.campaign.adapter.beatkind.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatKindJpaRepository extends JpaRepository<BeatKindJpaEntity, UUID> {

    List<BeatKindJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<BeatKindJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    Optional<BeatKindJpaEntity> findByWorldIdAndNameIgnoreCase(UUID worldId, String name);
}
