package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import com.campaignorganizer.interchange.foundry.application.port.out.FoundryConnectionRepositoryPort;
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FoundryConnectionPersistenceAdapter implements FoundryConnectionRepositoryPort {

    private final FoundryConnectionJpaRepository repository;
    private final FoundryConnectionPersistenceMapper mapper;

    public FoundryConnectionPersistenceAdapter(FoundryConnectionJpaRepository repository,
                                               FoundryConnectionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<FoundryConnection> findByWorldId(UUID worldId) {
        return repository.findById(worldId).map(mapper::toDomain);
    }

    @Override
    public FoundryConnection save(FoundryConnection connection) {
        return mapper.toDomain(repository.save(mapper.toEntity(connection)));
    }
}
