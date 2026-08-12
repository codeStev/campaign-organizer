package com.campaignorganizer.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<Campaign> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
