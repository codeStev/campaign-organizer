package com.campaignorganizer.tables.application.category.port.in;

import java.util.UUID;

public interface DeleteTableDeckCategoryUseCase {

    void delete(UUID worldId, UUID categoryId);
}
