package com.campaignorganizer.characters.application.statblock.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
