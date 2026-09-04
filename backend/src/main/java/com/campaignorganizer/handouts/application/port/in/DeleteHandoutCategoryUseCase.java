package com.campaignorganizer.handouts.application.port.in;

import java.util.UUID;

public interface DeleteHandoutCategoryUseCase {

    void delete(UUID worldId, UUID categoryId);
}
