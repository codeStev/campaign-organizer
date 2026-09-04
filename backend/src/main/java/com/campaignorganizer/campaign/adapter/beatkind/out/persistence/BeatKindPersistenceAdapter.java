package com.campaignorganizer.campaign.adapter.beatkind.out.persistence;

import com.campaignorganizer.campaign.application.beatkind.port.out.BeatKindRepositoryPort;
import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BeatKindPersistenceAdapter implements BeatKindRepositoryPort {

    private final BeatKindJpaRepository repository;
    private final BeatKindPersistenceMapper mapper;

    public BeatKindPersistenceAdapter(BeatKindJpaRepository repository, BeatKindPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<BeatKind> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<BeatKind> findByIdAndWorld(UUID beatKindId, UUID worldId) {
        return repository.findByIdAndWorldId(beatKindId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<BeatKind> findByNameIgnoreCaseAndWorld(String name, UUID worldId) {
        return repository.findByWorldIdAndNameIgnoreCase(worldId, name).map(mapper::toDomain);
    }

    @Override
    public BeatKind save(BeatKind beatKind) {
        return mapper.toDomain(repository.save(mapper.toEntity(beatKind)));
    }

    @Override
    public void delete(BeatKind beatKind) {
        repository.deleteById(beatKind.getId());
    }
}
