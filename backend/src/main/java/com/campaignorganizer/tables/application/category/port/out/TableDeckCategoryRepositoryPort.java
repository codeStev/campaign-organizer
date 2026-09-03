package com.campaignorganizer.tables.application.category.port.out;

import com.campaignorganizer.tables.domain.category.TableDeckCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableDeckCategoryRepositoryPort {

    List<TableDeckCategory> findByWorldOrderByName(UUID worldId);

    Optional<TableDeckCategory> findByIdAndWorld(UUID categoryId, UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);

    TableDeckCategory save(TableDeckCategory category);

    void delete(TableDeckCategory category);
}
