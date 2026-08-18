package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.UUID;

public interface DeleteCategoryUseCase {

    void delete(UUID worldId, UUID categoryId);
}
