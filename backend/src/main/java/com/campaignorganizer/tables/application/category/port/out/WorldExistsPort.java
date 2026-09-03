package com.campaignorganizer.tables.application.category.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
