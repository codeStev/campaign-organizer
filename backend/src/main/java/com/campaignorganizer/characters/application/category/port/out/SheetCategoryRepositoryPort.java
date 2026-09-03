package com.campaignorganizer.characters.application.category.port.out;

import com.campaignorganizer.characters.domain.category.SheetCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SheetCategoryRepositoryPort {

    List<SheetCategory> findByWorldOrderByName(UUID worldId);

    Optional<SheetCategory> findByIdAndWorld(UUID categoryId, UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);

    SheetCategory save(SheetCategory category);

    void delete(SheetCategory category);
}
