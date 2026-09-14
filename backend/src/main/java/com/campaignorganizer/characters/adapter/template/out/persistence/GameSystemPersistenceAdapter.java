package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.application.template.port.out.GameSystemRepositoryPort;
import com.campaignorganizer.characters.domain.template.GameSystem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GameSystemPersistenceAdapter implements GameSystemRepositoryPort {

    private final GameSystemJpaRepository repository;
    private final GameSystemPersistenceMapper mapper;

    public GameSystemPersistenceAdapter(GameSystemJpaRepository repository,
                                        GameSystemPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<GameSystem> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerIdOrderByNameAsc(ownerId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GameSystem> findById(UUID systemId) {
        return repository.findById(systemId).map(mapper::toDomain);
    }

    @Override
    public Optional<GameSystem> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name) {
        return repository.findByOwnerIdAndNameIgnoreCase(ownerId, name).map(mapper::toDomain);
    }

    @Override
    public GameSystem save(GameSystem system) {
        return mapper.toDomain(repository.save(mapper.toEntity(system)));
    }

    @Override
    public void delete(GameSystem system) {
        repository.deleteById(system.getId());
    }

    @Override
    public void assignUnownedTo(UUID ownerId) {
        repository.assignUnownedTo(ownerId);
    }
}
