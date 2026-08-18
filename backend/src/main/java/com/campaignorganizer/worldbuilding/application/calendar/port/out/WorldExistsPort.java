package com.campaignorganizer.worldbuilding.application.calendar.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
