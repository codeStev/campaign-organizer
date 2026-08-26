package com.campaignorganizer.handouts.application.port.in;

import java.util.UUID;

public interface DeleteHandoutUseCase {

    void delete(UUID worldId, UUID handoutId);
}
