package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import com.campaignorganizer.interchange.foundry.application.port.out.FoundryPushRecordRepositoryPort;
import com.campaignorganizer.interchange.foundry.domain.FoundryEntityType;
import com.campaignorganizer.interchange.foundry.domain.FoundryPushRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FoundryPushRecordPersistenceAdapter implements FoundryPushRecordRepositoryPort {

    private final FoundryPushRecordJpaRepository repository;
    private final FoundryPushRecordPersistenceMapper mapper;

    public FoundryPushRecordPersistenceAdapter(FoundryPushRecordJpaRepository repository,
                                               FoundryPushRecordPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<FoundryPushRecord> findByEntity(UUID worldId, FoundryEntityType entityType, UUID entityId) {
        return repository.findByWorldIdAndEntityTypeAndEntityId(worldId, entityType.name(), entityId)
                .map(mapper::toDomain);
    }

    @Override
    public FoundryPushRecord save(FoundryPushRecord record) {
        return mapper.toDomain(repository.save(mapper.toEntity(record)));
    }
}
