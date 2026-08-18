package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import com.campaignorganizer.campaign.application.campaign.port.out.CampaignRepositoryPort;
import com.campaignorganizer.campaign.domain.campaign.Campaign;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CampaignPersistenceAdapter implements CampaignRepositoryPort {

    private final CampaignJpaRepository repository;
    private final CampaignPersistenceMapper mapper;

    public CampaignPersistenceAdapter(CampaignJpaRepository repository, CampaignPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Campaign> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Campaign> findByIdAndWorld(UUID campaignId, UUID worldId) {
        return repository.findByIdAndWorldId(campaignId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<Campaign> findById(UUID campaignId) {
        return repository.findById(campaignId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID campaignId, UUID worldId) {
        return repository.existsByIdAndWorldId(campaignId, worldId);
    }

    @Override
    public Campaign save(Campaign campaign) {
        return mapper.toDomain(repository.save(mapper.toEntity(campaign)));
    }

    @Override
    public void delete(Campaign campaign) {
        repository.deleteById(campaign.getId());
    }
}
