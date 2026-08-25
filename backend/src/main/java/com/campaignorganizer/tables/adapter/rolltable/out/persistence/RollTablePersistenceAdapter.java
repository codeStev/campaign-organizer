package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

import com.campaignorganizer.tables.application.rolltable.port.out.RollTableRepositoryPort;
import com.campaignorganizer.tables.domain.rolltable.RollTable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the roll-table repository port. */
@Component
public class RollTablePersistenceAdapter implements RollTableRepositoryPort {

    private final RollTableJpaRepository repository;
    private final RollTablePersistenceMapper mapper;

    public RollTablePersistenceAdapter(RollTableJpaRepository repository,
                                       RollTablePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<RollTable> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RollTable> findByIdAndWorld(UUID tableId, UUID worldId) {
        return repository.findByIdAndWorldId(tableId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<RollTable> findById(UUID tableId) {
        return repository.findById(tableId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID tableId, UUID worldId) {
        return repository.existsByIdAndWorldId(tableId, worldId);
    }

    @Override
    public RollTable save(RollTable table) {
        return mapper.toDomain(repository.save(mapper.toEntity(table)));
    }

    @Override
    public void delete(RollTable table) {
        repository.deleteById(table.getId());
    }
}
