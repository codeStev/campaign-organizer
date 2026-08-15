package com.campaignorganizer.worldbuilding.application.timeline.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
