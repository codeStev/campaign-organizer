package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface GetFoundryConnectionUseCase {

    Optional<FoundryConnectionView> get(UUID worldId);
}
