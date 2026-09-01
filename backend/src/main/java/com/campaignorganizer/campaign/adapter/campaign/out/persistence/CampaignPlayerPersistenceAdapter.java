package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import com.campaignorganizer.campaign.application.campaign.port.out.CampaignPlayerRepositoryPort;
import com.campaignorganizer.campaign.domain.campaign.CampaignPlayer;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the campaign-roster repository port. */
@Component
public class CampaignPlayerPersistenceAdapter implements CampaignPlayerRepositoryPort {

    private final CampaignPlayerJpaRepository repository;
    private final CampaignPlayerPersistenceMapper mapper;

    public CampaignPlayerPersistenceAdapter(CampaignPlayerJpaRepository repository,
                                            CampaignPlayerPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CampaignPlayer> findByCampaign(UUID campaignId) {
        return repository.findByCampaignId(campaignId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public CampaignPlayer save(CampaignPlayer entry) {
        return mapper.toDomain(repository.save(mapper.toEntity(entry)));
    }

    @Override
    public void deleteByCampaign(UUID campaignId) {
        repository.deleteByCampaignId(campaignId);
    }
}
