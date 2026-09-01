package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import com.campaignorganizer.characters.application.statblock.port.out.GlobalStatblockRepositoryPort;
import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GlobalStatblockPersistenceAdapter implements GlobalStatblockRepositoryPort {

    private final GlobalStatblockJpaRepository repository;
    private final GlobalStatblockPersistenceMapper mapper;

    public GlobalStatblockPersistenceAdapter(GlobalStatblockJpaRepository repository,
                                             GlobalStatblockPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<GlobalStatblock> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<GlobalStatblock> findBySystemId(UUID systemId) {
        return repository.findBySystemIdOrderByCreatedAtDesc(systemId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GlobalStatblock> findById(UUID globalStatblockId) {
        return repository.findById(globalStatblockId).map(mapper::toDomain);
    }

    @Override
    public Optional<GlobalStatblock> findBySystemIdAndName(UUID systemId, String name) {
        return repository.findBySystemIdAndName(systemId, name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByGlobalTemplateId(UUID globalTemplateId) {
        return repository.existsByGlobalTemplateId(globalTemplateId);
    }

    @Override
    public GlobalStatblock save(GlobalStatblock statblock) {
        return mapper.toDomain(repository.save(mapper.toEntity(statblock)));
    }

    @Override
    public void delete(GlobalStatblock statblock) {
        repository.deleteById(statblock.getId());
    }
}
