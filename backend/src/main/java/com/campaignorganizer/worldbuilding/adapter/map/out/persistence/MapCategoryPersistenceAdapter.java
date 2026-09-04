package com.campaignorganizer.worldbuilding.adapter.map.out.persistence;

import com.campaignorganizer.worldbuilding.application.map.port.out.MapCategoryRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.map.MapCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MapCategoryPersistenceAdapter implements MapCategoryRepositoryPort {

    private final MapCategoryJpaRepository repository;
    private final MapCategoryPersistenceMapper mapper;

    public MapCategoryPersistenceAdapter(MapCategoryJpaRepository repository, MapCategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<MapCategory> findByWorldOrderByName(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<MapCategory> findByIdAndWorld(UUID categoryId, UUID worldId) {
        return repository.findByIdAndWorldId(categoryId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return repository.existsByIdAndWorldId(categoryId, worldId);
    }

    @Override
    public MapCategory save(MapCategory category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public void delete(MapCategory category) {
        repository.deleteById(category.getId());
    }
}
