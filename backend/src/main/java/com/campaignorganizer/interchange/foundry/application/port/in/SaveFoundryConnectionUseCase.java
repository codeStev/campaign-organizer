package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface SaveFoundryConnectionUseCase {

    FoundryConnectionView save(UUID worldId, SaveFoundryConnectionCommand command);

    /** An empty {@code apiKey} means "keep the currently stored key" — the caller
     * isn't forced to re-enter it on every save. */
    record SaveFoundryConnectionCommand(String relayBaseUrl, String clientId, Optional<String> apiKey) {
    }
}
