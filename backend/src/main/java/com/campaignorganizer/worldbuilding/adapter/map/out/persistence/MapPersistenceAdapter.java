package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.application.map.port.out.MapRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.map.WorldMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MapPersistenceAdapter implements MapRepositoryPort {

    private final WorldMapJpaRepository repository;
    private final MapPersistenceMapper mapper;

    public MapPersistenceAdapter(WorldMapJpaRepository repository, MapPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<WorldMap> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WorldMap> findByIdAndWorld(UUID mapId, UUID worldId) {
        return repository.findByIdAndWorldId(mapId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<WorldMap> findById(UUID mapId) {
        return repository.findById(mapId).map(mapper::toDomain);
    }

    @Override
    public WorldMap save(WorldMap map) {
        return mapper.toDomain(repository.save(mapper.toEntity(map)));
    }

    @Override
    public void delete(WorldMap map) {
        repository.deleteById(map.getId());
    }
}
