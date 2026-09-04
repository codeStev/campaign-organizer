package com.campaignorganizer.characters.application.category.port.in;

import java.util.UUID;

public interface DeleteSheetCategoryUseCase {

    void delete(UUID worldId, UUID categoryId);
}
