package com.campaignorganizer.worldbuilding.adapter.world.out.persistence;

import com.campaignorganizer.worldbuilding.application.world.port.out.WorldRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.world.World;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class WorldPersistenceAdapter implements WorldRepositoryPort {

    private final WorldJpaRepository repository;
    private final WorldPersistenceMapper mapper;

    public WorldPersistenceAdapter(WorldJpaRepository repository, WorldPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<World> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<World> findById(UUID worldId) {
        return repository.findById(worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(UUID worldId) {
        return repository.existsById(worldId);
    }

    @Override
    public World save(World world) {
        return mapper.toDomain(repository.save(mapper.toEntity(world)));
    }

    @Override
    public void delete(World world) {
        repository.deleteById(world.getId());
    }
}
