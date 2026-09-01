package com.campaignorganizer.campaign.adapter.player.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerJpaRepository extends JpaRepository<PlayerJpaEntity, UUID> {

    List<PlayerJpaEntity> findByWorldIdOrderByNameAsc(UUID worldId);

    Optional<PlayerJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
