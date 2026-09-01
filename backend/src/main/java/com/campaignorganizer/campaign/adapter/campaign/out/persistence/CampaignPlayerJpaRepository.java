package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignPlayerJpaRepository extends JpaRepository<CampaignPlayerJpaEntity, UUID> {

    List<CampaignPlayerJpaEntity> findByCampaignId(UUID campaignId);

    void deleteByCampaignId(UUID campaignId);
}
