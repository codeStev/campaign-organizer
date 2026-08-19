package com.campaignorganizer.characters.application.sheet.port.out;

import java.util.UUID;

public interface WorldExistsPort {

    boolean exists(UUID worldId);
}
