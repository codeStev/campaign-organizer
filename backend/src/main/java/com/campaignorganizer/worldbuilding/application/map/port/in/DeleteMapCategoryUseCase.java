package com.campaignorganizer.worldbuilding.application.map.port.in;

import java.util.UUID;

public interface DeleteMapCategoryUseCase {

    void delete(UUID worldId, UUID categoryId);
}
