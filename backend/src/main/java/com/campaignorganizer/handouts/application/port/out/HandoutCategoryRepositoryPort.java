package com.campaignorganizer.handouts.application.port.out;

import com.campaignorganizer.handouts.domain.HandoutCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HandoutCategoryRepositoryPort {

    List<HandoutCategory> findByWorldOrderByName(UUID worldId);

    Optional<HandoutCategory> findByIdAndWorld(UUID categoryId, UUID worldId);

    boolean existsInWorld(UUID categoryId, UUID worldId);

    HandoutCategory save(HandoutCategory category);

    void delete(HandoutCategory category);
}
