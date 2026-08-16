package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.CategoryRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository repository;
    private final CategoryPersistenceMapper mapper;

    public CategoryPersistenceAdapter(CategoryJpaRepository repository, CategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Category> findByWorldOrderByName(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findByIdAndWorld(UUID categoryId, UUID worldId) {
        return repository.findByIdAndWorldId(categoryId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return repository.existsByIdAndWorldId(categoryId, worldId);
    }

    @Override
    public Category save(Category category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public void delete(Category category) {
        repository.deleteById(category.getId());
    }
}
