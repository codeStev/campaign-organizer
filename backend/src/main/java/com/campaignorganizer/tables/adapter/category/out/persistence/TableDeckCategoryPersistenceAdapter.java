package com.campaignorganizer.tables.adapter.category.out.persistence;

import com.campaignorganizer.tables.application.category.port.out.TableDeckCategoryRepositoryPort;
import com.campaignorganizer.tables.domain.category.TableDeckCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TableDeckCategoryPersistenceAdapter implements TableDeckCategoryRepositoryPort {

    private final TableDeckCategoryJpaRepository repository;
    private final TableDeckCategoryPersistenceMapper mapper;

    public TableDeckCategoryPersistenceAdapter(TableDeckCategoryJpaRepository repository,
                                              TableDeckCategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<TableDeckCategory> findByWorldOrderByName(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TableDeckCategory> findByIdAndWorld(UUID categoryId, UUID worldId) {
        return repository.findByIdAndWorldId(categoryId, worldId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID categoryId, UUID worldId) {
        return repository.existsByIdAndWorldId(categoryId, worldId);
    }

    @Override
    public TableDeckCategory save(TableDeckCategory category) {
        return mapper.toDomain(repository.save(mapper.toEntity(category)));
    }

    @Override
    public void delete(TableDeckCategory category) {
        repository.deleteById(category.getId());
    }
}
