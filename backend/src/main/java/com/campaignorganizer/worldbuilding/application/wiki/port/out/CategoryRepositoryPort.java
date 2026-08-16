package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    List<Category> findByWorldOrderByName(UUID worldId);

    Optional<Category> findByIdAndWorld(UUID categoryId, UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);

    Category save(Category category);

    void delete(Category category);
}
