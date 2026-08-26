package com.campaignorganizer.handouts.adapter.out.persistence;

import com.campaignorganizer.handouts.application.port.out.HandoutRepositoryPort;
import com.campaignorganizer.handouts.domain.Handout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the handout repository port. */
@Component
public class HandoutPersistenceAdapter implements HandoutRepositoryPort {

    private final HandoutJpaRepository repository;
    private final HandoutPersistenceMapper mapper;

    public HandoutPersistenceAdapter(HandoutJpaRepository repository,
                                     HandoutPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Handout> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Handout> findByIdAndWorld(UUID handoutId, UUID worldId) {
        return repository.findByIdAndWorldId(handoutId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<Handout> findById(UUID handoutId) {
        return repository.findById(handoutId).map(mapper::toDomain);
    }

    @Override
    public Handout save(Handout handout) {
        return mapper.toDomain(repository.save(mapper.toEntity(handout)));
    }

    @Override
    public void delete(Handout handout) {
        repository.deleteById(handout.getId());
    }
}
