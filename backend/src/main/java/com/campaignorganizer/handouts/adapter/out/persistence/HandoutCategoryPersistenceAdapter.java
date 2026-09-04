package com.campaignorganizer.handouts.adapter.out.persistence;

import com.campaignorganizer.handouts.application.port.out.HandoutCategoryRepositoryPort;
import com.campaignorganizer.handouts.domain.HandoutCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class HandoutCategoryPersistenceAdapter implements HandoutCategoryRepositoryPort {

    private final HandoutCategoryJpaRepository repository;
    private final HandoutCategoryPersistenceMapper mapper;

    public HandoutCategoryPersistenceAdapter(HandoutCategoryJpaRepository repository,
                                              HandoutCategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<HandoutCategory> findByWorldOrderByName(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<HandoutCategory> findByIdAndWorld(UUID categoryId, UUID worldId) {
        return repository.findByIdAndWorldId(categoryId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return repository.existsByIdAndWorldId(categoryId, worldId);
    }

    @Override
    public HandoutCategory save(HandoutCategory category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public void delete(HandoutCategory category) {
        repository.deleteById(category.getId());
    }
}
