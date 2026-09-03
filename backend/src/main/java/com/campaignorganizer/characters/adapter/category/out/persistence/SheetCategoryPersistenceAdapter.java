package com.campaignorganizer.characters.adapter.category.out.persistence;

import com.campaignorganizer.characters.application.category.port.out.SheetCategoryRepositoryPort;
import com.campaignorganizer.characters.domain.category.SheetCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SheetCategoryPersistenceAdapter implements SheetCategoryRepositoryPort {

    private final SheetCategoryJpaRepository repository;
    private final SheetCategoryPersistenceMapper mapper;

    public SheetCategoryPersistenceAdapter(SheetCategoryJpaRepository repository,
                                              SheetCategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<SheetCategory> findByWorldOrderByName(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SheetCategory> findByIdAndWorld(UUID categoryId, UUID worldId) {
        return repository.findByIdAndWorldId(categoryId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return repository.existsByIdAndWorldId(categoryId, worldId);
    }

    @Override
    public SheetCategory save(SheetCategory category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public void delete(SheetCategory category) {
        repository.deleteById(category.getId());
    }
}
