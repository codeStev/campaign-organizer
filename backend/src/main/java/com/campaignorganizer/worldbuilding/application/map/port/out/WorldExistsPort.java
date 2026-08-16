package com.campaignorganizer.worldbuilding.application.map.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
