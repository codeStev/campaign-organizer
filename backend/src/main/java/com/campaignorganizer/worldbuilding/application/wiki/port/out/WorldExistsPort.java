package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
