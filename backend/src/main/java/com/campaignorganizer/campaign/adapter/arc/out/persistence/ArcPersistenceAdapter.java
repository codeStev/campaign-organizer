package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import com.campaignorganizer.campaign.application.arc.port.out.ArcRepositoryPort;
import com.campaignorganizer.campaign.domain.arc.Arc;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArcPersistenceAdapter implements ArcRepositoryPort {

    private final ArcJpaRepository repository;
    private final ArcPersistenceMapper mapper;

    public ArcPersistenceAdapter(ArcJpaRepository repository, ArcPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Arc> findByCampaign(UUID campaignId) {
        return repository.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Arc> findByIdAndCampaign(UUID arcId, UUID campaignId) {
        return repository.findByIdAndCampaignId(arcId, campaignId).map(mapper::toDomain);
    }

    @Override
    public Optional<Arc> findById(UUID arcId) {
        return repository.findById(arcId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInCampaign(UUID arcId, UUID campaignId) {
        return repository.existsByIdAndCampaignId(arcId, campaignId);
    }

    @Override
    public Arc save(Arc arc) {
        return mapper.toDomain(repository.save(mapper.toEntity(arc)));
    }

    @Override
    public void delete(Arc arc) {
        repository.deleteById(arc.getId());
    }
}
