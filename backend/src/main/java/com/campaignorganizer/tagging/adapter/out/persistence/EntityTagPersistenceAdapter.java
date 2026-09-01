package com.campaignorganizer.tagging.adapter.out.persistence;

import com.campaignorganizer.tagging.application.port.out.EntityTagRepositoryPort;
import com.campaignorganizer.tagging.domain.EntityTag;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** JPA-backed implementation of the entity-tag repository port. */
@Component
public class EntityTagPersistenceAdapter implements EntityTagRepositoryPort {

    private final EntityTagJpaRepository repository;
    private final EntityTagPersistenceMapper mapper;

    public EntityTagPersistenceAdapter(EntityTagJpaRepository repository,
                                       EntityTagPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<EntityTag> findByWorld(UUID worldId) {
        return repository.findByWorldId(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<EntityTag> findByEntity(UUID worldId, EntityType entityType, UUID entityId) {
        return repository.findByWorldIdAndEntityTypeAndEntityId(worldId, entityType, entityId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UUID> findEntityIdsByWorldAndTypeAndName(UUID worldId, EntityType entityType,
                                                          String name) {
        return repository.findEntityIdsByWorldIdAndEntityTypeAndName(worldId, entityType, name);
    }

    @Override
    public List<UUID> findEntityIdsByWorldAndTypeAndNameContaining(UUID worldId,
            EntityType entityType, String fragment) {
        return repository.findEntityIdsByWorldIdAndEntityTypeAndNameContaining(worldId, entityType,
                fragment);
    }

    @Override
    public List<String> findDistinctNamesByWorld(UUID worldId) {
        return repository.findDistinctNamesByWorldId(worldId);
    }

    @Override
    public EntityTag save(EntityTag tag) {
        return mapper.toDomain(repository.save(mapper.toEntity(tag)));
    }

    @Override
    public void deleteByEntity(UUID worldId, EntityType entityType, UUID entityId) {
        repository.deleteByWorldIdAndEntityTypeAndEntityId(worldId, entityType, entityId);
    }
}
