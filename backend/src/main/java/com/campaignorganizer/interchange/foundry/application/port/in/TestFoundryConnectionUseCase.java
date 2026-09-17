package com.campaignorganizer.interchange.foundry.application.port.in;

import java.util.List;
import java.util.UUID;

public interface TestFoundryConnectionUseCase {

    FoundryConnectionTestView test(UUID worldId);

    record FoundryConnectionTestView(boolean ok, List<String> connectedClientIds, String error) {
    }
}
