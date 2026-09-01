package com.campaignorganizer.campaign.adapter.encounter.out.persistence;

import com.campaignorganizer.campaign.application.encounter.port.out.EncounterRepositoryPort;
import com.campaignorganizer.campaign.domain.encounter.Encounter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EncounterPersistenceAdapter implements EncounterRepositoryPort {

    private final EncounterJpaRepository repository;
    private final EncounterPersistenceMapper mapper;

    public EncounterPersistenceAdapter(EncounterJpaRepository repository, EncounterPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Encounter> findByCampaign(UUID campaignId) {
        return repository.findByCampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Encounter> findByIdAndCampaign(UUID encounterId, UUID campaignId) {
        return repository.findByIdAndCampaignId(encounterId, campaignId).map(mapper::toDomain);
    }

    @Override
    public Optional<Encounter> findById(UUID encounterId) {
        return repository.findById(encounterId).map(mapper::toDomain);
    }

    @Override
    public Encounter save(Encounter encounter) {
        return mapper.toDomain(repository.save(mapper.toEntity(encounter)));
    }

    @Override
    public void delete(Encounter encounter) {
        repository.deleteById(encounter.getId());
    }
}
