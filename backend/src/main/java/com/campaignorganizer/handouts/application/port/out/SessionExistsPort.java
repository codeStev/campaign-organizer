package com.campaignorganizer.handouts.application.port.out;

import java.util.UUID;

public interface SessionExistsPort {

    boolean existsInWorld(UUID sessionId, UUID worldId);
}
