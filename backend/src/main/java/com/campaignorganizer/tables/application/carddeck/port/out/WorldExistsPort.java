package com.campaignorganizer.tables.application.carddeck.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
