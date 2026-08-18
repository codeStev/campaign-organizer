package com.campaignorganizer.worldbuilding.application.relationship.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
